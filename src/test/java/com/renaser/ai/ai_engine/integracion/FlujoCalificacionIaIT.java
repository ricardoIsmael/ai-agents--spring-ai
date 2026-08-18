package com.renaser.ai.ai_engine.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renaser.ai.ai_engine.ai.dto.RespuestaModelo;
import com.renaser.ai.ai_engine.ai.service.ClienteModelo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * La calificación con inteligencia artificial, de punta a punta.
 *
 * <p>Es lo último que le faltaba al hito 2: hasta ahora el candidato entregaba su evaluación,
 * el código puntuaba lo cerrado y la postulación se quedaba en {@code PERFIL_CALIFICANDO}
 * para siempre, porque nadie ejecutaba a los tres agentes.
 *
 * <p><b>El modelo está sustituido por un doble, y su hermana no.</b> Aquí se comprueban las
 * reglas del sistema —la cola, los reintentos, la bitácora, qué se guarda y qué se descarta—,
 * y para eso hace falta que la respuesta del modelo sea siempre la misma: con respuestas que
 * cambian cada vez no se puede afirmar nada. Además incluye a propósito respuestas
 * <b>malas</b> —un criterio que no existe, una nota sin explicación, una respuesta ajena— que
 * un modelo de verdad no devolvería a demanda, y que son justo las que hay que descartar.
 *
 * <p>Que el proveedor conteste de verdad y que lo que conteste encaje en el contrato lo
 * comprueba {@code CalificacionIaRealIT}, que sí llama a DeepSeek.
 *
 * <p>Se comprueban las cuatro que sostienen esta parte:
 * <ul>
 *   <li>La IA nunca ve foto, edad, sexo ni estado civil (RF-41).
 *   <li>Toda llamada al modelo queda en {@code ejecucion_ia}, salga bien o salga mal (RF-146).
 *   <li>Una nota sin explicación no se guarda (RF-150).
 *   <li>Si la IA falla se reintenta y <b>no se inventa una nota</b> (Regla 3 del doc 03).
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlujoCalificacionIaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("pgvector/pgvector:pg16");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @TempDir
    static Path carpetaArchivos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.archivos.ruta", () -> carpetaArchivos.toString());
        registro.add("app.seguridad.jwt-secreto",
                () -> "clave-de-pruebas-suficientemente-larga-para-hmac-256-bits");
        registro.add("spring.ai.deepseek.api-key", () -> "clave-de-pruebas-no-se-usa");
        // Dos intentos y no tres: la prueba del fallo tiene que agotarlos, y cada intento
        // vuelve a publicar el mensaje al momento.
        registro.add("renaser.ai.calificacion.max-intentos", () -> "2");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    final ObjectMapper json = new ObjectMapper();

    static String tokenEquipo;
    static long vacanteId;
    static String codigoBueno;
    static String codigoFallido;

    // ============ El doble del modelo ============

    /**
     * Sustituye a DeepSeek. Devuelve una respuesta creíble por agente y sabe fallar a
     * propósito, que es lo que permite comprobar la regla del reintento.
     */
    @TestConfiguration
    static class ConfiguracionDePrueba {
        @Bean
        @Primary
        ClienteModelo clienteModeloDePrueba() {
            return new ModeloDePrueba();
        }
    }

    static class ModeloDePrueba implements ClienteModelo {

        static volatile boolean falla = false;
        static final List<String> enviosRecibidos = new CopyOnWriteArrayList<>();

        private static final Pattern RESPUESTA_ID = Pattern.compile("\"respuestaId\"\\s*:\\s*(\\d+)");

        @Override
        public RespuestaModelo preguntar(String agenteCodigo, String instruccion, String contenido) {
            enviosRecibidos.add(instruccion + "\n" + contenido);
            if (falla) {
                throw new IllegalStateException("el proveedor del modelo no responde");
            }
            return new RespuestaModelo(respuestaDe(agenteCodigo, contenido),
                    "deepseek-v4-flash", "deepseek", "prueba", 1200, 340);
        }

        private String respuestaDe(String agenteCodigo, String contenido) {
            return switch (agenteCodigo) {
                case "EVIDENCIA_CV" -> evidenciaCv();
                case "EVALUADOR" -> evaluador(contenido);
                case "POTENCIAL_RIESGO" -> potencialRiesgo();
                default -> throw new IllegalStateException("agente inesperado: " + agenteCodigo);
            };
        }

        private String evidenciaCv() {
            List<String> criterios = List.of("CV_RESULTADOS", "CV_COMPLEJIDAD", "CV_SISTEMAS",
                    "CV_PERSONAS", "CV_APRENDIZAJE", "CV_INICIATIVA", "CV_HABILIDADES",
                    "CV_EVIDENCIA");
            List<String> filas = new ArrayList<>();
            for (String codigo : criterios) {
                filas.add(("""
                        {"codigo":"%s","puntaje":75,"explicacion":"Hay un resultado medido y \
                        atribuible.","evidencia":"pasó de 3 días a 4 horas"}""").formatted(codigo));
            }
            // Una novena nota, de un criterio que no existe: tiene que descartarse sola.
            filas.add("""
                    {"codigo":"CV_INVENTADO","puntaje":99,"explicacion":"no existe","evidencia":null}""");
            // Y una décima sin explicación: RF-150 dice que no se guarda.
            filas.add("""
                    {"codigo":"CV_RESULTADOS","puntaje":10,"explicacion":"","evidencia":null}""");
            return """
                    {"criterios":[%s],
                     "afirmaciones":[
                       {"texto":"Automatizó el cierre mensual","clasificacion":"DEMOSTRADA",
                        "preguntaValidacion":null},
                       {"texto":"Lideró un equipo","clasificacion":"DECLARADA",
                        "preguntaValidacion":"¿A cuántas personas y durante cuánto tiempo?"},
                       {"texto":"Algo raro","clasificacion":"NO_EXISTE","preguntaValidacion":null}],
                     "confianza":72}""".formatted(String.join(",", filas));
        }

        private String evaluador(String contenido) {
            List<String> notas = new ArrayList<>();
            Matcher ids = RESPUESTA_ID.matcher(contenido);
            while (ids.find()) {
                notas.add(("""
                        {"respuestaId":%s,"puntaje":3,"explicacion":"Actuó por iniciativa y midió \
                        el resultado.","evidenciaCitada":"reduje el tiempo de cierre","confianza":68}""")
                        .formatted(ids.group(1)));
            }
            // Una nota para una respuesta de otra persona: no puede colarse.
            notas.add("""
                    {"respuestaId":999999,"puntaje":4,"explicacion":"ajena","evidenciaCitada":"x",
                     "confianza":90}""");
            return "{\"notas\":[%s]}".formatted(String.join(",", notas));
        }

        private String potencialRiesgo() {
            return """
                    {"adecuacion":78,"potencial":81,"altoRendimiento":70,"confianzaEvidencia":66,
                     "resumen":"Perfil sólido en ejecución, con resultados medidos y poca evidencia \
                     sobre trabajo con personas.",
                     "hallazgos":[
                       {"tipo":"FORTALEZA","descripcion":"Mide lo que hace",
                        "evidencia":"pasó de 3 días a 4 horas","esCanalizable":false,"sugerencia":null},
                       {"tipo":"FALTA_EVIDENCIA","descripcion":"No hay ejemplos de haber formado a nadie",
                        "evidencia":null,"esCanalizable":false,
                        "sugerencia":"Preguntárselo en la conversación final"},
                       {"tipo":"INVENTADO","descripcion":"tipo que no existe","evidencia":null,
                        "esCanalizable":false,"sugerencia":null}],
                     "alertas":[
                       {"tipo":"DEMASIADO_IDEAL",
                        "descripcion":"Ninguna respuesta reconoce un límite propio"}]}""";
        }
    }

    // ============ Las pruebas ============

    @Test
    @Order(1)
    void laIaCalificaYLaPostulacionAvanzaSola() throws Exception {
        ModeloDePrueba.falla = false;
        ModeloDePrueba.enviosRecibidos.clear();

        tokenEquipo = entrarComoEquipo();
        vacanteId = prepararVacantePublicada();
        codigoBueno = postularConCurriculumReal("camila@correo.pe");
        responderYEntregar(codigoBueno);

        // La cola arrancó sola al entregar: nadie tuvo que pulsar nada
        esperarA(() -> contar("select count(*) from trabajo_ia where estado = 'TERMINADO'") == 3,
                "los tres agentes terminen");

        long postulacionId = idDe(codigoBueno);

        // 1 · La postulación avanzó a donde decide una persona, y tiene grupo de prioridad
        Map<String, Object> postulacion = jdbc.queryForMap(
                "select estado_codigo, grupo_prioridad from postulacion where id = ?", postulacionId);
        assertThat(postulacion.get("estado_codigo")).isEqualTo("PERFIL_POR_CONFIRMAR");
        assertThat(postulacion.get("grupo_prioridad"))
                .isIn("ALTA", "POTENCIAL_CON_RIESGO", "NO_PRIORIZADO");

        // 2 · La IA leyó un currículum SIN edad, sexo ni estado civil (RF-41). Se comprueba
        // sobre el envío literal que quedó en la bitácora, que es lo que de verdad salió.
        String envio = jdbc.queryForObject(
                "select envio from ejecucion_ia where agente_codigo = 'EVIDENCIA_CV'", String.class);
        assertThat(envio)
                .doesNotContain("34 años")
                .doesNotContain("Femenino")
                .doesNotContain("Casada")
                .doesNotContain("1992");
        // Pero sí llevaba lo que hay que puntuar: si no, la nota no valdría nada
        assertThat(envio).contains("cierre mensual");

        // Y las dos versiones quedaron guardadas, que es lo que permite demostrarlo después
        Map<String, Object> cv = jdbc.queryForMap(
                "select texto_extraido, texto_anonimizado from cv where postulacion_id = ?", postulacionId);
        assertThat((String) cv.get("texto_extraido")).contains("34");
        assertThat((String) cv.get("texto_anonimizado")).doesNotContain("34 años");

        // 3 · Toda llamada quedó en la bitácora, con su instrucción y su versión (RF-146)
        List<Map<String, Object>> ejecuciones = jdbc.queryForList("""
                select agente_codigo, version_agente, instruccion_ia_id, es_exitosa,
                       tokens_entrada, duracion_ms
                from ejecucion_ia order by id""");
        assertThat(ejecuciones).hasSize(3);
        assertThat(ejecuciones).allSatisfy(fila -> {
            assertThat(fila.get("instruccion_ia_id")).isNotNull();
            assertThat(fila.get("version_agente")).isNotNull();
            assertThat(fila.get("es_exitosa")).isEqualTo(true);
            assertThat(fila.get("tokens_entrada")).isNotNull();
        });
        assertThat(ejecuciones.stream().map(f -> f.get("agente_codigo")).toList())
                .containsExactly("EVIDENCIA_CV", "EVALUADOR", "POTENCIAL_RIESGO");

        // 4 · Los ocho criterios del currículum, ni uno más. El noveno que devolvió el modelo
        // no existe y el décimo venía sin explicación: los dos se descartan (RF-150).
        assertThat(contar("select count(*) from nota_criterio where postulacion_id = " + postulacionId))
                .isEqualTo(8);
        assertThat(contar("""
                select count(*) from nota_criterio
                where postulacion_id = %d and origen = 'AGENTE'
                  and explicacion is not null and btrim(explicacion) <> ''
                  and ejecucion_ia_id is not null""".formatted(postulacionId))).isEqualTo(8);

        // Las afirmaciones clasificadas: la tercera traía una clasificación inventada
        assertThat(contar("select count(*) from afirmacion_cv")).isEqualTo(2);

        // 5 · Las respuestas abiertas calificadas de 0 a 4, y ninguna ajena
        int abiertas = contar("""
                select count(*) from respuesta r
                join pregunta p on p.id = r.pregunta_id
                join evaluacion e on e.id = r.evaluacion_id
                join postulacion po on po.evaluacion_id = e.id
                where po.id = %d and r.opcion_id is null and p.es_puntuable""".formatted(postulacionId));
        assertThat(abiertas)
                .withFailMessage("La plantilla de Ejecución tiene que traer preguntas abiertas; "
                        + "sin ellas esta prueba no comprueba nada del EVALUADOR")
                .isGreaterThan(0);
        assertThat(contar("select count(*) from nota_respuesta")).isEqualTo(abiertas);
        assertThat(contar("select count(*) from nota_respuesta where puntaje between 0 and 4"))
                .isEqualTo(abiertas);

        // 6 · El Perfil de Talento, que no es una nota sino un retrato (RF-65)
        Map<String, Object> perfil = jdbc.queryForMap("""
                select adecuacion, potencial, confianza_evidencia, resumen, version_pesos_id,
                       ejecucion_ia_id
                from perfil_talento where postulacion_id = ?""", postulacionId);
        assertThat(perfil.get("confianza_evidencia")).isNotNull();
        assertThat(perfil.get("version_pesos_id")).isNotNull();
        assertThat((String) perfil.get("resumen")).isNotBlank();

        // Los hallazgos no se mezclan, y el tipo inventado se descartó
        List<String> tipos = jdbc.queryForList("select tipo from hallazgo_perfil", String.class);
        assertThat(tipos).containsExactlyInAnyOrder("FORTALEZA", "FALTA_EVIDENCIA");

        // La alerta de «demasiado ideal» quedó, y no descarta a nadie: la postulación sigue viva
        assertThat(contar("select count(*) from alerta where tipo = 'DEMASIADO_IDEAL'")).isEqualTo(1);

        // 7 · La nota de la etapa se rehízo con currículum + evaluación, atada a la versión de
        // pesos de la vacante (no a la última publicada)
        Map<String, Object> nota = jdbc.queryForMap("""
                select puntaje, version_pesos_id from nota_etapa
                where postulacion_id = ? and etapa_codigo = 'PERFIL_INTEGRAL'""", postulacionId);
        assertThat(((Number) nota.get("puntaje")).doubleValue()).isBetween(0.01, 100.0);
        Long versionDeLaVacante = jdbc.queryForObject(
                "select version_pesos_id from vacante where id = ?", Long.class, vacanteId);
        assertThat(nota.get("version_pesos_id")).isEqualTo(versionDeLaVacante);
    }

    @Test
    @Order(2)
    void siLaIaFallaSeReintentaYNoSeInventaNingunaNota() throws Exception {
        ModeloDePrueba.falla = true;

        codigoFallido = postularConCurriculumReal("luis@correo.pe");
        responderYEntregar(codigoFallido);
        long postulacionId = idDe(codigoFallido);

        esperarA(() -> contar("""
                select count(*) from trabajo_ia
                where postulacion_id = %d and estado = 'FALLIDO'""".formatted(postulacionId)) == 1,
                "el trabajo se dé por fallido tras agotar los intentos");

        // Se intentó dos veces, que es el tope configurado
        assertThat(contar("select intentos from trabajo_ia where postulacion_id = " + postulacionId))
                .isEqualTo(2);

        // Los dos intentos quedaron en la bitácora, con su error. Un fallo sin rastro es lo
        // que impide después saber si la IA está rota o si el candidato no dio para más.
        List<Map<String, Object>> fallos = jdbc.queryForList("""
                select es_exitosa, error from ejecucion_ia
                where trabajo_ia_id in (select id from trabajo_ia where postulacion_id = %d)"""
                .formatted(postulacionId));
        assertThat(fallos).hasSize(2);
        assertThat(fallos).allSatisfy(fila -> {
            assertThat(fila.get("es_exitosa")).isEqualTo(false);
            assertThat((String) fila.get("error")).contains("no responde");
        });

        // Y lo que importa: NO se inventó ninguna nota, y la postulación sigue esperando
        assertThat(contar("select count(*) from nota_criterio where postulacion_id = " + postulacionId))
                .isZero();
        assertThat(contar("select count(*) from perfil_talento where postulacion_id = " + postulacionId))
                .isZero();
        assertThat(jdbc.queryForObject(
                "select estado_codigo from postulacion where id = ?", String.class, postulacionId))
                .isEqualTo("PERFIL_CALIFICANDO");
        assertThat(jdbc.queryForObject(
                "select grupo_prioridad from postulacion where id = ?", String.class, postulacionId))
                .isNull();

        // El segundo y el tercer agente ni se crearon: la fila se corta donde falla
        assertThat(contar("select count(*) from trabajo_ia where postulacion_id = " + postulacionId))
                .isEqualTo(1);

        ModeloDePrueba.falla = false;
    }

    // ============ Apoyo ============

    /**
     * Un PDF de verdad, con los datos que la IA no puede ver.
     *
     * <p>Tiene que ser un PDF real y no unos bytes cualesquiera: lo que se está probando es
     * justamente que del archivo se saque texto y que ese texto salga recortado.
     */
    private byte[] curriculumEnPdf() throws Exception {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            List<String> lineas = List.of(
                    "Camila Rojas - Analista de procesos",
                    "Edad: 34 años",
                    "Sexo: Femenino",
                    "Estado civil: Casada",
                    "Fecha de nacimiento: 12/03/1992",
                    "",
                    "EXPERIENCIA",
                    "Automatice el cierre mensual y paso de 3 dias a 4 horas.",
                    "Documente el proceso y lo dejo funcionando sin mi.",
                    "Aprendi SQL en dos meses para poder medir el resultado.");
            try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina)) {
                lienzo.beginText();
                lienzo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                lienzo.setLeading(16f);
                lienzo.newLineAtOffset(50, 720);
                for (String linea : lineas) {
                    lienzo.showText(linea);
                    lienzo.newLine();
                }
                lienzo.endText();
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private String postularConCurriculumReal(String correo) throws Exception {
        String tokenCandidato = crearCandidatoYEntrar(correo);
        MockMultipartFile cv = new MockMultipartFile("cv", "cv.pdf", "application/pdf",
                curriculumEnPdf());
        String codigo = leer(mvc.perform(multipart("/api/v1/portal/postulaciones")
                        .file(cv)
                        .param("vacanteId", String.valueOf(vacanteId))
                        .param("resultadoOrgulloso",
                                "Automaticé el cierre mensual y pasó de 3 días a 4 horas")
                        .header("Authorization", "Bearer " + tokenCandidato))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "codigo");
        tokensPorCodigo.put(codigo, tokenCandidato);
        return codigo;
    }

    private final Map<String, String> tokensPorCodigo = new java.util.HashMap<>();

    private void responderYEntregar(String codigo) throws Exception {
        String token = tokensPorCodigo.get(codigo);
        String cuerpo = mvc.perform(post("/api/v1/portal/evaluacion/" + codigo + "/inicio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode preguntas = json.readTree(cuerpo).get("preguntas");
        for (JsonNode pregunta : preguntas) {
            JsonNode opciones = pregunta.get("opciones");
            String respuesta = opciones != null && !opciones.isEmpty()
                    ? "{\"opcionId\":%d,\"segundos\":30}".formatted(opciones.get(0).get("id").asLong())
                    : "{\"texto\":\"Un caso concreto: reduje el tiempo de cierre midiendo antes y "
                            + "después, y dejé el proceso documentado\",\"segundos\":90}";
            mvc.perform(put("/api/v1/portal/evaluacion/" + codigo + "/respuestas/"
                            + pregunta.get("id").asLong())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(respuesta))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/v1/portal/evaluacion/" + codigo + "/entrega")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** La cola es asíncrona: hay que esperarla, no adivinar cuánto tarda. */
    private void esperarA(BooleanSupplier condicion, String que) {
        long limite = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        fail("Se agotó la espera a que " + que + ". Trabajos: "
                + jdbc.queryForList("select id, agente_codigo, estado, intentos from trabajo_ia")
                + " · Ejecuciones: "
                + jdbc.queryForList("select agente_codigo, es_exitosa, error from ejecucion_ia"));
    }

    private int contar(String sql) {
        Integer valor = jdbc.queryForObject(sql, Integer.class);
        return valor == null ? 0 : valor;
    }

    private long idDe(String codigo) {
        return jdbc.queryForObject("select id from postulacion where uuid = ?::uuid", Long.class, codigo);
    }

    private String entrarComoEquipo() throws Exception {
        return leer(mvc.perform(post("/api/v1/panel/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioRenaserOsId\":\"dev-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "token");
    }

    private long prepararVacantePublicada() throws Exception {
        jdbc.update("INSERT INTO area (organizacion_id, nombre, es_activa) VALUES (1, 'Tecnología', true)");
        Long areaId = jdbc.queryForObject("SELECT id FROM area LIMIT 1", Long.class);

        long solicitudId = Long.parseLong(leer(conToken(post("/api/v1/panel/solicitudes"), tokenEquipo, """
                {"areaId": %d, "urgencia": "NORMAL",
                 "nivelPuestoCodigo": "EJECUCION", "familiaCodigo": "TECNOLOGIA",
                 "resultadoPrincipal": "Sostener el portal",
                 "motivo": "No se llega a los plazos",
                 "consecuenciaNoContratar": "Se retrasa el MVP",
                 "analisisCapacidad": "Se evaluó automatizar y no alcanza",
                 "responsableUsuarioId": 1,
                 "resultadosEsperados": [
                   {"descripcion": "Publicar el portal", "indicador": "en producción"},
                   {"descripcion": "Reducir bugs", "indicador": "la mitad"},
                   {"descripcion": "Documentar", "indicador": "docs al día"}
                 ]}""".formatted(areaId))
                .andReturn().getResponse().getContentAsString(), "id"));

        conToken(post("/api/v1/panel/solicitudes/" + solicitudId + "/aprobacion"), tokenEquipo,
                "{\"motivo\":\"Hay presupuesto\"}").andExpect(status().isOk());

        long puestoId = Long.parseLong(leer(conToken(post("/api/v1/panel/puestos"), tokenEquipo, """
                {"codigo": "DEV_WEB", "nombre": "Desarrollador web",
                 "nivelPuestoCodigo": "EJECUCION", "familiaCodigo": "TECNOLOGIA"}""")
                .andReturn().getResponse().getContentAsString(), "id"));

        long id = Long.parseLong(leer(conToken(post("/api/v1/panel/vacantes"), tokenEquipo, """
                {"solicitudTalentoId": %d, "puestoId": %d,
                 "titulo": "Desarrollador web", "descripcion": "Portal de talento",
                 "tipoCierre": "PERMANENTE", "responsableUsuarioId": 1}"""
                .formatted(solicitudId, puestoId))
                .andReturn().getResponse().getContentAsString(), "id"));

        Long plantillaId = jdbc.queryForObject(
                "select id from plantilla_evaluacion where nivel_puesto_codigo = 'EJECUCION'", Long.class);
        conToken(post("/api/v1/panel/vacantes/" + id + "/plantilla-evaluacion"), tokenEquipo,
                "{\"plantillaEvaluacionId\": %d}".formatted(plantillaId)).andExpect(status().isOk());

        Long versionPruebaId = armarUnaPruebaValida(tokenEquipo);
        conToken(post("/api/v1/panel/vacantes/" + id + "/plantilla-prueba"), tokenEquipo,
                "{\"versionPlantillaPruebaId\": %d}".formatted(versionPruebaId)).andExpect(status().isOk());

        conToken(post("/api/v1/panel/vacantes/" + id + "/publicacion"), tokenEquipo, null)
                .andExpect(status().isOk());
        return id;
    }

    private String crearCandidatoYEntrar(String correo) throws Exception {
        mvc.perform(post("/api/v1/portal/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"nombre":"Camila","apellidos":"Rojas","correo":"%s",
                         "contrasena":"unaClaveLarga123","aceptaProceso":true,
                         "aceptaFuturosContactos":false}""".formatted(correo)))
                .andExpect(status().isCreated());
        return leer(mvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"unaClaveLarga123\"}".formatted(correo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "token");
    }

    // La mínima prueba del puesto publicable, que la vacante exige para publicarse
    private Long armarUnaPruebaValida(String token) throws Exception {
        long plantillaId = Long.parseLong(leer(conToken(post("/api/v1/panel/plantillas-prueba"), token,
                "{\"nombre\":\"Prueba genérica\"}")
                .andReturn().getResponse().getContentAsString(), "id"));
        long versionId = Long.parseLong(leer(conToken(
                post("/api/v1/panel/plantillas-prueba/" + plantillaId + "/versiones"), token, """
                {"enunciado":"Resuelve el caso propuesto","modalidad":"CRONOMETRADA",
                 "duracionMinutos":90,"minutoCambioMin":30,"minutoCambioMax":50,"minutosExtra":10}""")
                .andReturn().getResponse().getContentAsString(), "id"));

        for (int i = 0; i < 8; i++) {
            long id = Long.parseLong(leer(conToken(post("/api/v1/panel/plantillas-prueba/preguntas"), token,
                    "{\"codigo\":\"UNIV_IA_%d\",\"enunciado\":\"Pregunta universal %d\",\"tipo\":\"UNIVERSAL\"}"
                            .formatted(i, i))
                    .andReturn().getResponse().getContentAsString(), "id"));
            conToken(post("/api/v1/panel/plantillas-prueba/versiones/" + versionId + "/preguntas"), token,
                    "{\"preguntaPruebaId\": %d}".formatted(id)).andExpect(status().isOk());
        }
        for (int i = 0; i < 3; i++) {
            long id = Long.parseLong(leer(conToken(post("/api/v1/panel/plantillas-prueba/preguntas"), token,
                    "{\"codigo\":\"ESP_IA_%d\",\"enunciado\":\"Pregunta específica %d\",\"tipo\":\"ESPECIFICA\"}"
                            .formatted(i, i))
                    .andReturn().getResponse().getContentAsString(), "id"));
            conToken(post("/api/v1/panel/plantillas-prueba/versiones/" + versionId + "/preguntas"), token,
                    "{\"preguntaPruebaId\": %d}".formatted(id)).andExpect(status().isOk());
        }
        conToken(post("/api/v1/panel/plantillas-prueba/versiones/" + versionId + "/rubrica"), token, """
                {"codigo":"RESULTADO_IA","nombre":"Resultado","puntos":100,"metodoVerificacion":"PERSONA"}""")
                .andExpect(status().isCreated());

        conToken(post("/api/v1/panel/plantillas-prueba/versiones/" + versionId + "/publicacion"), token, null)
                .andExpect(status().isOk());
        return versionId;
    }

    private ResultActions conToken(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
            String token, String cuerpo) throws Exception {
        peticion.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
        if (cuerpo != null) peticion.content(cuerpo);
        return mvc.perform(peticion);
    }

    private String leer(String cuerpoRespuesta, String campo) throws Exception {
        return json.readTree(cuerpoRespuesta).get(campo).asText();
    }
}
