## Documentacion Generada
Generar la documentacion detallada, con palabras basicas del español, breve. Diseñado para que el usuario lo pueda leer y entender. El objetivo de los documentos no es que sean grandes, sino, entendibles.

## Respuestas
Responde en el chat de manera breve, corta y directa, eliminando puntos innecesarios y ambiguedades, tambien eliminando partes de código amenos que sea muy necesario o la pregunta del usuario sea muy especifica del código. Si para 
implementar algo es necesario alguna acción por parte de mi, como por ejemplo: Ejecutar un script en la base de datos. Entonces me das una sección que se llamará "Flujo de Implementación" en la cual me darás los pasos especificos que 
debo de cumplir en orden para poder realizar la implementación adecuadamente. Si tengo que tomar decisiones damelos en una nueva sección que diga "Decisiones" para asi evitar confundirlo con texto normal, dentro de esa sección me dirás lo que tengo que decidir o la informacion que tengo que darte.

## Como preguntar
No uses el vocabulario de los documentos del cliente al hacer preguntas: sus terminos son
ambiguos (ej. "corte", "gate", "avanzar"). Interpreta el significado con todo el contexto y
pregunta en palabras propias y concretas, describiendo la situacion real.

## El backend
El repositorio del backend es este mismo directorio: `ai-engine`, un solo proyecto para todo
el sistema (reclutamiento y agentes de IA juntos).

| | |
|---|---|
| Java | 25 |
| Spring Boot | 4.1 |
| Base de datos | PostgreSQL propio con pgvector, en `docker-compose.yml`, puerto 5433 |
| Cola | RabbitMQ, para calificar en segundo plano |
| IA | Spring AI con **Ollama**, corriendo en el propio servidor |
| Otros | MapStruct, Lombok, hilos virtuales |

Todo corre en servidores de Renaser: los datos de candidatos no salen a terceros.
Flyway, Security, JWT, Validation y Swagger ya estan en el `pom.xml` y compilan.

## RENASER OS
Es un proyecto avanzado y este backend es un modulo suyo. Son **dos servicios separados que
hablan por HTTP**: no comparten base de datos, asi que no hay claves foraneas contra sus tablas.

| Que | Como |
|---|---|
| Identidad del equipo de Renaser | RENASER OS emite el token; aqui solo se valida. No guardamos su contrasena |
| Identidad de los candidatos | Cuenta propia en esta base. No son usuarios de RENASER OS |
| Permisos del modulo | Nuestros. Su sistema no conoce acciones como «publicar una version del banco» |
| Archivos y entregables | Almacen propio, con enlaces firmados de corta duracion |
| Tareas, tiempos, bloqueos, retrabajo | Se leen de su API. Alimentan las metricas de la validacion |
| Objetivos y resultados del personal | Se leen de su API. Es la fuente del seguimiento 30/90/180 |

## Donde estamos (15/08/2026)
**Documentacion cerrada, codigo sin empezar.**

| | |
|---|---|
| Documentacion | Los diez documentos de `docs/` estan al dia y verificados entre si |
| Base de datos | **Vacia. No hay ni una migracion de Flyway escrita todavia** |
| Codigo | El esqueleto de Spring Boot compila; no hay ninguna entidad ni endpoint del dominio |
| Mockups | Los mantiene otra persona y **describen la version anterior** |

Lo siguiente es el **hito 1 del [documento 08](docs/08-ALCANCE-DEL-MVP.md)**: 27 tablas, sin nada
de IA. Antes de escribir la primera migracion, leer ese documento entero — sobre todo «Lo que se
deja preparado para no reescribir despues», que dice que cosas del sistema completo tienen que
existir desde el primer dia aunque el hito 1 no las use.

## Documentos del proyecto
Todo vive en `docs/`. Empezar por [docs/README.md](docs/README.md).

- [Que hace el sistema](docs/00-QUE-HACE-EL-SISTEMA.md) — el sistema entero **sin una sola palabra
  tecnica**, en cinco minutos. **Empezar por aqui.** Si algo de los demas documentos no se puede
  amarrar a una frase de este, sobra.
- [Requisitos funcionales](docs/01-REQUISITOS-FUNCIONALES.md) — RF-01 a RF-154. Objetivo, alcance,
  que hace el sistema y donde interviene una persona. Desarrolla cada frase del 00. Se lee solo.
- [Requisitos no funcionales](docs/02-REQUISITOS-NO-FUNCIONALES.md) — RNF-01 a RNF-66.
- [Estados de la postulacion](docs/03-ESTADOS-POSTULACION.md) — los **18** estados y sus
  transiciones. Base del backend: ningun estado fuera de esa lista puede existir. Tienen forma
  de rejilla (cinco etapas por cuatro momentos), asi que el siguiente estado se calcula.
  **Ojo: la maquina de estados es propuesta nuestra, no viene de Renaser.**
- [Roles y permisos](docs/04-ROLES-Y-PERMISOS.md) — los **73** permisos, accion por accion.
  Un rol es un conjunto de permisos guardado en base de datos, no algo fijo en codigo.
- [Modelo de datos](docs/05-MODELO-DE-DATOS.md) — las **93** tablas por area, que impide la base
  y que tiene que vivir en el codigo. Se lee para entender el modelo.
- [Diccionario de datos](docs/07-DICCIONARIO-DE-DATOS.md) — cada tabla con todas sus columnas,
  tipos y claves. Se consulta. **Es la base de las migraciones de Flyway.**
- [Alcance del MVP](docs/08-ALCANCE-DEL-MVP.md) — **el cliente quiere un MVP, no el sistema
  completo.** Tres hitos: 27 tablas, 59 y 70, cada uno usable solo. Dice tambien que se deja
  puesto desde el hito 1 para no reescribir despues. **Leerlo antes de escribir cualquier
  migracion.**
- `docs/diagramas/` — HTML autocontenidos, se abren en el navegador.
- `docs/mockups/` — prototipos de pantalla. **Los mantiene otra persona, no editarlos.**
- `docs/insumos/` — documentos originales del cliente y notas de analisis.

`DESIGN.md` y `PRODUCT.md` en la raiz son de la herramienta de mockups, no de este trabajo.

## Al editar los documentos
- Cada documento debe leerse solo: quien lo abra no necesita otro para entenderlo.
  Los 01 a 05 abren con contexto propio y cierran con enlaces a los demas.
- **Nunca citar un requisito por su numero entre documentos.** Los numeros cambian cada vez
  que se inserta uno y las referencias quedan apuntando a otra cosa sin que nada falle.
  Citar el nombre de la seccion: `(ver «Panel de gestion»)`, `(ver «Auditoria» en los no
  funcionales)`. Los numeros RF/RNF solo existen dentro de su propio documento.
- Verificar que las secciones citadas existan:
  `grep -ohE '«[^»]+»' docs/0[3-4]*.md | sort -u`

## Diagramas
Usar la skill `diagram-design`. Paleta tomada de RENASER OS: papel `#f5f5f3`,
tinta `#0d0d0d`, apagado `#70706b`, linea `#e3e3df`, y el semaforo del propio sistema
verde `#2f6e51` / ambar `#9b6a22` / rojo `#a43b36`.
Verificar siempre con `self_check.py` y ademas revisar el render: `google-chrome --headless
--screenshot` genera un PNG que hay que mirar. El verificador no detecta flechas que cruzan
por detras de una caja ni texto desbordado.

## Insumos del cliente
`docs/insumos/` guarda transcripciones, documentos del cliente y notas de analisis.

Documentos clave (leer antes de trabajar):
- [Que cambia con el documento nuevo](docs/insumos/CAMBIOS-DEL-DOCUMENTO-NUEVO.md) — que trajo
  el documento del 14/08, que contradice y **que se decidio en cada punto**.
- [Que documento manda](docs/insumos/ANALISIS-DOCUMENTOS.md) — orden de precedencia y estado de
  cada documento del repositorio.
- [Decisiones tomadas](docs/insumos/DECISIONES.md) — arquitectura, embudo, roles y reglas ya cerradas.
  ⚠️ Todavia describe la version anterior en varios puntos.
- [La comprobacion](docs/insumos/COMPROBACION-SIN-TECNICA.md) — el rastreo de las 93 tablas
  contra el documento 00, y de donde salen los conteos de cada hito.
- [Notas temporales](docs/insumos/NOTAS-TEMPORALES.md) — lo que sigue pendiente.
- `docs/insumos/pruebas-tecnicas/` — **las cinco pruebas del puesto reales**, en PDF, tal como se
  enviaron a candidatos. **Son anteriores**: modelo de contenido y de tono, no de formato.
- [Entrevista con el cliente 08/08](docs/insumos/entrevista-cliente-2026-08-08.md) — transcripcion.

Regla de precedencia: manda **nuevo_doc_requisitos_funcionales.docx** (RENASER Talent
Intelligence V1.1 FINAL), que se declara definitivo y reemplaza a los anteriores.
`Sistema_Completo_Talento_RENASER_Seleccion_2026_2029` conserva valor para el texto completo de
bancos, pruebas y ejemplos que el vigente no reproduce, si no lo contradice.
`Banco_Maestro_Preguntas` sigue vigente solo para las preguntas y sus claves.
`Sistema_RENASER_Talent_Intelligence` es la version anterior del vigente y esta descartada: sus
baterias reutilizan los mismos IDs (D01-D90 etc.) para preguntas distintas.

## Lo que Renaser hace hoy, y lo que nunca ha hecho
El proceso **ya existe y lo llevan a mano**: no se esta inventando un metodo, se esta
digitalizando uno que usan. Eso vale para casi todo el recorrido, con **tres excepciones en la
prueba del puesto** que hay que tener presentes porque el modelo las soporta y la realidad no las
usa todavia:

| Lo que pide el documento del cliente | Lo que Renaser hace hoy | Estado |
|---|---|---|
| Prueba cronometrada, con reloj en el servidor | Encargo de varios dias, sin reloj | **Se hara.** Es la mejora que quieren |
| Un cambio inesperado a mitad de la prueba | No existe | **Se hara** |
| Rubrica publicada que sume 100 puntos | Una lista de 10 a 12 criterios **sin numeros** | **Pendiente de Renaser.** Frena el paso 0 |

Las cinco pruebas reales estan en `docs/insumos/pruebas-tecnicas/` (ARQ, BIO, CIVIL, CX, SIS: el
prefijo es la familia de trabajo). Son **anteriores**: valen como modelo de contenido y de tono,
no de formato. De mirarlas salio que los entregables sean una tabla y no un texto —cada prueba
pide de uno a cuatro, cada uno con su regla— y que `version_plantilla_prueba` tenga `modalidad`,
que existe solo para poder cargarlas y adaptarlas.

⚠️ **Una prueba cronometrada dura 60-120 minutos y lo que piden esas cinco no cabe ahi.** Un
producto funcional mas un video no se hace en dos horas. Ponerles reloj obliga a **encoger el
encargo**, y eso lo tiene que reescribir Renaser: sin eso el hito 3 no tiene contenido.

## Sistema susceptible a cambios
El cliente realiza cambios recurrentemente, ya sea en métricas, en datos, en permisos de roles, diseñar el sistema para que admita todo tipo de cambios inesperados, y que todo sea personalizable para el rol de dirección o admin. Que pueda ver y editar todo. 

