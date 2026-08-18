# Ruta de lectura del backend

Los archivos del backend en el orden en que conviene abrirlos: del más fácil al más difícil.
Cada uno se entiende con lo que viste en los anteriores. No hace falta leer nada más.

Contado sobre la rama `feat/talentov2`, commit `hito 3`:

| | |
|---|---|
| Clases Java | **344**: 113 del motor de agentes y 231 de selección de personal |
| Migraciones | V1 a V15, **76 tablas** |
| Controladores | 15 tuyos, 4 del motor de agentes |
| Tests | 9 archivos |

⚠️ Los documentos de `docs/` describen el sistema completo (93 tablas, 73 permisos). El código
va por detrás y por otro camino en algunos puntos. **Cuando se contradigan, manda el código.**

---

## Cómo usar esta ruta

Abres el archivo, lo lees, y preguntas lo que no entiendas. El número de líneas está para que
sepas a qué te enfrentas: nada de lo primero pasa de 50 líneas.

Marca aquí lo que vayas terminando.

---

## Etapa 0 · Qué se levanta al arrancar

Tres archivos, ninguno es Java difícil. Sirven para saber qué hay encendido.

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 1 | `AiEngineApplication.java` | 13 | El punto de entrada. Todo Spring Boot empieza así |
| 2 | `src/main/resources/application.yaml` | — | Base de datos, puertos, claves. Aquí se configura todo |
| 3 | `docker-compose.yml` | — | Postgres en el 5433 y RabbitMQ. Lo que tienes que tener corriendo |

Al terminar sabrás: **por qué la aplicación no arranca sin `DEEPSEEK_API_KEY`.**

## Etapa 1 · Las tres piezas mínimas

Una entidad, un repositorio y un DTO. Con esto ya puedes leer el 60% del proyecto.

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 4 | `organizacion/entity/Organizacion.java` | 21 | Una clase Java atada a una tabla |
| 5 | `organizacion/repository/OrganizacionRepository.java` | 11 | Una interfaz vacía que ya sabe consultar. Nadie escribe el SQL |
| 6 | `solicitud/entity/ResultadoEsperado.java` | 23 | Otra entidad, con una relación a su padre |
| 7 | `catalogo/dto/DtosCatalogo.java` | 32 | Los `record` que viajan al frontend |

Ábrelos junto a `db/migration/V2__identidad_y_permisos.sql`: ahí está la tabla que la entidad
copia. **La tabla manda, la clase obedece.**

## Etapa 2 · Tu primer dominio entero

`solicitud` es el dominio más pequeño y completo. Léelo en este orden exacto: es el mismo
recorrido que hace una petición HTTP.

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 8 | `solicitud/entity/SolicitudTalento.java` | 41 | La entidad principal |
| 9 | `solicitud/repository/SolicitudTalentoRepository.java` | 15 | Consultas propias, escritas como nombres de método |
| 10 | `solicitud/dto/DtosSolicitud.java` | 48 | Lo que entra y lo que sale |
| 11 | `solicitud/service/ServicioSolicitudes.java` | 21 | La interfaz: qué se puede hacer |
| 12 | `solicitud/controller/SolicitudesController.java` | 62 | Los endpoints |
| 13 | `solicitud/service/impl/ServicioSolicitudesImpl.java` | 137 | **La lógica de verdad.** El primero difícil |

Al terminar sabrás: **por qué el servicio tiene interfaz e implementación separadas.**

## Etapa 3 · Seguridad

Nueve archivos, de menor a mayor. Es el dominio que más se malentiende.

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 14 | `seguridad/dto/FiltroAlcance.java` | 18 | Hasta dónde puede ver alguien |
| 15 | `seguridad/config/PropiedadesSeguridad.java` | 23 | La configuración con tipos |
| 16 | `seguridad/dto/ContextoUsuario.java` | 25 | Quién es el usuario durante una petición |
| 17 | `seguridad/service/ServicioContexto.java` | 45 | Quién lo pone ahí |
| 18 | `seguridad/service/Permisos.java` | 46 | **`alcanceDe`: lo importante del dominio** |
| 19 | `seguridad/service/ServicioToken.java` | 47 | Firmar y verificar el JWT |
| 20 | `seguridad/filter/FiltroIdentidad.java` | 66 | Lo que corre *antes* del controlador |
| 21 | `seguridad/controller/PanelAuthController.java` | 84 | El `dev-login` provisional |
| 22 | `seguridad/config/ConfiguracionSeguridad.java` | 94 | Qué URL es pública y cuál no |

Al terminar sabrás: **por qué el alcance se aplica dentro de la consulta y no filtrando después.**

## Etapa 4 · La máquina de estados

El corazón del negocio. 18 estados, y una sola puerta para cambiar de uno a otro.

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 23 | `postulacion/entity/EstadoPostulacion.java` | 29 | Los estados como filas, no como enum |
| 24 | `postulacion/entity/TransicionEstado.java` | 32 | El historial que no se borra |
| 25 | `postulacion/entity/Postulacion.java` | 34 | La entidad central del sistema |
| 26 | `postulacion/service/MaquinaEstados.java` | 185 | **Las reglas.** Léelo con el test al lado |
| 27 | `test/postulacion/service/MaquinaEstadosTest.java` | — | Las reglas escritas como ejemplos |

## Etapa 5 · El recorrido del candidato

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 28 | `portal/dto/DtosPortal.java` | 50 | Lo que ve el candidato |
| 29 | `portal/controller/PortalController.java` | 119 | Los 12 endpoints públicos y con token |
| 30 | `portal/service/impl/ServicioPortalImpl.java` | 374 | **El archivo más grande de selección** |

## Etapa 6 · El panel del equipo

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 31 | `postulacion/repository/PostulacionRepository.java` | 48 | El alcance metido en las consultas |
| 32 | `postulacion/controller/PostulacionesPanelController.java` | 83 | La otra puerta |
| 33 | `postulacion/service/impl/ServicioPostulacionesPanelImpl.java` | 230 | Confirmar, ordenar, avanzar por lote |

## Etapa 7 · Las dos clases frontera

| | Archivo | Líneas | Qué vas a ver |
|---|---|---|---|
| 34 | `comun/exception/ManejadorErrores.java` | 106 | Tus errores en vez de un 500 mudo |
| 35 | `comun/config/ConfiguracionSwagger.java` | 90 | El candado, solo en tus endpoints |

Las dos llevan **una lista de controladores escrita a mano**. Un controlador nuevo que no se
sume aquí falla en silencio.

## Etapa 8 · Hito 2 · el Perfil Integral

67 clases. El mismo patrón de la etapa 2, repetido. Orden sugerido: `BancoPreguntasController`
→ `PlantillasEvaluacionController` → `EvaluacionPortalController` → `ServicioEvaluacionImpl` →
`ServicioCalificacionImpl`.

## Etapa 9 · Hito 3 · la prueba y la decisión

30 clases en `prueba` y 12 en `decision`, lo último construido (migración V15). `PruebaPortalController`
→ `ServicioPruebaImpl` → `CalificacionPruebaController` → `DecisionPanelController`.

## Etapa 10 · Los tests

| Archivo | Qué vas a ver |
|---|---|
| `integracion/MigracionesIT.java` | Las 15 migraciones contra un Postgres real |
| `integracion/FlujoHito1IT.java` | El hito 1 entero, de una pieza |
| `integracion/FlujoEvaluacionIT.java` | El Perfil Integral |
| `integracion/FlujoPruebaIT.java` | La prueba del puesto |

Es la forma más rápida de ver el sistema funcionando sin abrir el navegador.

## Etapa 11 · El motor de agentes

`ai/`, 113 clases. **No se toca**, pero comparte proceso, base de datos y puerto. Entrada:
`FlowController` → `AgentExecutionServiceImpl` → los prompts en `resources/prompts/`.

Dentro viven los 15 agentes y su respuesta estructurada, el encadenamiento por RabbitMQ con
`routing[]` y tope de profundidad, y el RAG con pgvector más los embeddings de Google Gemini.

---

## Lo que no existe todavía

| Hueco | Estado |
|---|---|
| Identidad del equipo | Hay un `dev-login`; el contrato con RENASER OS no existe |
| Correo | Se registra en `correo_enviado`, no se envía |
