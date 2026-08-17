# Las APIs del sistema

Sistema de selección de personal — Renaser Consulting
Versión 1.0 · 2026-08-15 · Cubre el **hito 1**

Este documento explica las APIs para quien las va a consumir: el frontend de RENASER OS y el
portal del candidato. **La referencia viva es Swagger**, en `http://localhost:8080/swagger-ui.html`
cuando la aplicación corre: ahí están los cuerpos exactos, se prueban las llamadas y siempre está
al día porque se genera del código. Este documento cuenta lo que Swagger no cuenta: cómo entrar,
qué puerta usar y las reglas que no se ven en un esquema.

---

## Las dos puertas

Todo vive bajo `/api/v1/`, en dos zonas con reglas distintas:

| Puerta | Quién la usa | Cómo se identifica |
|---|---|---|
| `/api/v1/portal/**` | El candidato | Token propio, de crear cuenta y entrar con correo y contraseña |
| `/api/v1/panel/**` | El equipo de Renaser | Token de equipo. Lo emitirá RENASER OS; mientras no exista ese contrato, el login de desarrollo |

El token va en cada llamada, en la cabecera `Authorization: Bearer <token>`.

Un token de candidato **no abre** el panel, ni al revés. Y dentro del panel, cada acción exige su
permiso: quien no lo tiene recibe un **403 con explicación**, no un error opaco. Además el
permiso tiene **alcance**: el responsable de un área solo ve las postulaciones de sus vacantes,
aunque llame al mismo endpoint que Talento.

## Cómo entrar

**El candidato:** `POST /portal/cuentas` para crear la cuenta (exige aceptar el tratamiento de
datos; el consentimiento de futuros contactos es aparte y opcional), y `POST /portal/auth/login`
con correo y contraseña. Tras varios intentos fallidos seguidos, la entrada se bloquea unos
minutos (configurable).

**El equipo, mientras no hay RENASER OS:** `POST /panel/auth/dev-login` con el id de RENASER OS.
El primer id que entre en una base recién creada se registra solo, con los roles completos del
equipo — es el arranque de desarrollo. En producción este login se apaga con
`app.seguridad.dev-login-activo: false`.

## Los errores hablan claro

Todos los errores salen en el mismo formato (RFC 7807): un `title`, un `status` y un `detail`
en lenguaje normal.

| Código | Qué significa |
|---|---|
| 400 | La petición incumple una regla: «toda transición manual exige un motivo escrito» |
| 401 | Falta el token o venció |
| 403 | El token vale, pero ese permiso no lo tienes |
| 404 | No existe, **o no te toca verlo**: el alcance también responde 404 |
| 409 | El estado actual no lo permite: «ya postulaste a esta vacante» |
| 413 | El archivo pasa de 10 MB |

---

## El portal del candidato (`/api/v1/portal`)

| Método y ruta | Qué hace | Quién |
|---|---|---|
| GET `/vacantes` | Las vacantes publicadas | Cualquiera, sin token |
| GET `/vacantes/{id}` | El detalle público, con los requisitos indispensables | Cualquiera |
| GET `/consentimientos/textos` | Los textos vigentes de los dos consentimientos | Cualquiera |
| POST `/cuentas` | Crear la cuenta y registrar los consentimientos | Cualquiera |
| POST `/auth/login` | Entrar; devuelve el token | Cualquiera |
| POST `/postulaciones` | Postular: CV (PDF o Word, máx. 10 MB), enlaces, el resultado del que se siente orgulloso, y la confirmación de los requisitos | Candidato |
| GET `/postulaciones` | Sus postulaciones, con estado y días sin cambio | Candidato |
| GET `/postulaciones/{uuid}` | El detalle de una suya, con el historial completo | Candidato |
| POST `/postulaciones/{uuid}/retiro` | Retirarla. **No borra sus datos**: eso se pide aparte | Candidato |
| POST `/consentimientos/futuros/retiro` | Retirar el consentimiento de futuros contactos | Candidato |
| POST `/solicitudes-borrado` | Pedir el borrado de sus datos | Candidato |

**La regla que importa al postular:** el formulario pregunta por cada requisito indispensable de
la vacante y el candidato confirma cuáles cumple. Cualquier requisito activo no confirmado cierra
la postulación en el acto (`NO_CONTINUA`), con la regla exacta escrita en su historial. Es el
**único** descarte automático de todo el sistema.

## El panel del equipo (`/api/v1/panel`)

### Solicitudes de Talento — Talento prepara, Dirección aprueba

| Método y ruta | Qué hace | Permiso |
|---|---|---|
| POST `/solicitudes` | Registrar una solicitud, con sus 3 a 5 resultados esperados y el análisis de capacidad (obligatorio) | `crear_solicitud` |
| GET `/solicitudes` · `/{id}` | Verlas, según el alcance de quien mira | `ver_solicitudes` |
| POST `/solicitudes/{id}/aprobacion` | Aprobar: queda ABIERTA y ya admite vacante | `aprobar_solicitud` |
| POST `/solicitudes/{id}/rechazo` | Rechazar, con motivo | `aprobar_solicitud` |

### Vacantes

| Método y ruta | Qué hace | Permiso |
|---|---|---|
| GET/POST `/puestos` | El catálogo de puestos | `ver_vacantes` / `crear_vacante` |
| GET `/vacantes` · `/{id}` | Todas las vacantes | `ver_vacantes` |
| POST `/vacantes` | Crear en borrador. **Exige una solicitud aprobada** | `crear_vacante` |
| PUT `/vacantes/{id}` | Editar mientras no esté cerrada | `editar_vacante` |
| GET/POST `/vacantes/{id}/requisitos` · DELETE `/{requisitoId}` | Los requisitos indispensables. No se borran: se desactivan | `definir_requisitos_objetivos` |
| POST `/vacantes/{id}/publicacion` | Publicar: aparece en el portal | `publicar_vacante` |
| POST `/vacantes/{id}/cierre` | Cerrar: frena postulaciones nuevas, **no arrastra las que van en marcha** | `cerrar_vacante` |

### Postulaciones

| Método y ruta | Qué hace | Permiso |
|---|---|---|
| GET `/bandeja?espera_a=` | La bandeja: todo lo que espera a `CANDIDATO`, `SISTEMA`, `TALENTO` o `AREA` | `ver_candidatos` |
| GET `/vacantes/{id}/embudo` | Cuántas postulaciones hay en cada estado | `ver_embudo` |
| GET `/postulaciones/{id}` · `/historial` | La ficha completa y el recorrido | `abrir_ficha_candidato` |
| POST `/postulaciones/{id}/transiciones` | Mover a cualquier estado. **El motivo es obligatorio, sin excepción** | `mover_postulacion` |
| POST `/postulaciones/{id}/confirmacion-avance` | Confirmar que avanza: el sistema calcula el estado siguiente | `confirmar_avance` |
| GET `/archivos/{id}/descarga` | Descargar el CV | `descargar_entregables` |

### Administración

| Método y ruta | Qué hace | Permiso |
|---|---|---|
| GET `/areas` · POST `/areas` | Las áreas de la organización: hace falta una para registrar una solicitud | `ver_solicitudes` / `crear_usuarios_y_asignar_roles` |
| GET/PUT `/parametros` | Los valores que Renaser cambia sin programar | `editar_parametros` |
| GET/POST `/plantillas-correo` | Los textos de correo. Editar = crear versión nueva | `editar_textos_correo` |
| GET `/auditoria` | El registro, paginado. No se puede modificar ni borrar | `ver_auditoria` |
| GET `/solicitudes-borrado` · POST `/{id}/ejecucion` | Ver y ejecutar los borrados: la persona queda vacía, la trazabilidad queda | `ejecutar_borrado_datos` |
| GET/POST `/usuarios` · POST `/{id}/roles` · GET `/roles` | El equipo y sus roles. El último administrador no se puede quitar | `crear_usuarios_y_asignar_roles` |

---

## Lo que conviene saber antes de consumirlas

**Los correos no salen todavía.** Cada aviso al candidato queda guardado con su texto exacto en
la base (`correo_enviado`), pero el envío real espera a que Renaser confirme su dominio de
correo. Cuando exista, se enchufa el transporte y nada más cambia.

**La prueba del puesto todavía no vive aquí.** Hoy se manda como PDF y se entrega por fuera,
como se venía haciendo. El sistema solo registra el movimiento del candidato por esa etapa, con
su motivo. La prueba dentro del sistema —cronómetro en el servidor, cambio a mitad y entregables
subidos— es el hito 3 (ver «Alcance del MVP»). Lo mismo vale para la simulación y la validación.

**El id público de una postulación es su `uuid`,** no el número interno. Es lo que ve el
candidato y lo que viaja en sus rutas.

**El módulo de agentes IA** (`/api/v1/agent-runs`, `/flows`, `/rag`, `/supabase`) es otra zona,
del proyecto original de agentes, y hoy queda abierta como estaba. Se endurecerá cuando gane
autenticación propia.

---

# Documentos relacionados

| Documento | Qué contiene |
|---|---|
| [Qué hace el sistema](00-QUE-HACE-EL-SISTEMA.md) | El sistema entero, sin nada técnico |
| [Alcance del MVP](08-ALCANCE-DEL-MVP.md) | Qué se construye primero y por qué |
| [Roles y permisos](04-ROLES-Y-PERMISOS.md) | Quién puede hacer qué, acción por acción |
| [Estados de la postulación](03-ESTADOS-POSTULACION.md) | Los estados que mueve esta API |
| [Diccionario de datos](07-DICCIONARIO-DE-DATOS.md) | Las tablas que hay detrás |
