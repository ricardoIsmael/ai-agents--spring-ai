# Diccionario de datos

Las 71 tablas del sistema de selección de Renaser, una por una, con todas sus columnas.

Este documento se consulta, no se lee de corrido. Para entender **por qué** el modelo es así
—las decisiones de diseño, qué impide la base y qué tiene que vivir en el código— está el
[Modelo de datos](05-MODELO-DE-DATOS.md).

---

## Cómo leer las tablas

| Columna del cuadro | Qué significa |
|---|---|
| **Columna** | El nombre exacto que llevará en PostgreSQL |
| **Tipo** | El tipo de PostgreSQL |
| **Oblig.** | Si admite quedar vacío. `sí` = `NOT NULL` |
| **Qué guarda** | Para qué sirve, en una línea |

Debajo de cada cuadro van las **claves**: la primaria, las foráneas que importan y las
combinaciones que no se pueden repetir.

### Convenciones que se repiten en todas

- **`id`** — `bigint`, generado por la base (`GENERATED ALWAYS AS IDENTITY`). No se reutiliza.
- **`creado_en`** — `timestamptz` con valor por defecto `now()`. Cuando no aporta nada se
  omite del cuadro, pero existe.
- **Todas las fechas con hora son `timestamptz`**, nunca `timestamp` a secas. Guardan la zona
  horaria porque de las marcas de la simulación salen las preguntas de la conversación final,
  y una hora sin zona no se puede comparar.
- **Los catálogos usan `codigo` de texto como clave**, no un número. Así se lee la tabla sin
  cruzarla con nada.
- **Los puntajes son `numeric`, nunca `float`.** Un decimal binario redondea mal y estas notas
  deciden si alguien entra a trabajar.
- **`es_activo`** en un catálogo significa que ya no se ofrece, pero lo que ya lo usó sigue
  apuntando ahí. Nunca se borra una fila de catálogo.

### Sobre los identificadores públicos

Las tablas que el portal público expone en una dirección web —`vacante`, `postulacion`— van a
necesitar además una columna `uuid` para no dejar adivinar cuántas hay ni entrar a la de otro
cambiando un número. Está anotado en cada una. No reemplaza al `id`: el `id` sigue siendo la
clave interna.

---

# 1 · Personas, acceso y permisos

## `persona`

Quién es alguien. La usan por igual el equipo de Renaser y quien postula.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `nombre` | text | no | Nombre de pila |
| `apellidos` | text | no | Apellidos |
| `telefono` | varchar(30) | no | Teléfono de contacto |
| `documento` | varchar(20) | no | DNI o carné de extranjería |
| `fecha_nacimiento` | date | no | Solo para requisitos legales, nunca para puntuar |
| `anonimizado_en` | timestamptz | no | Cuándo se ejecutó el borrado de sus datos |
| `creado_en` | timestamptz | sí | |

**Clave primaria:** `id`

Nombre y apellidos admiten quedar vacíos **solo porque el borrado los vacía**. Al crear la
persona son obligatorios; eso lo exige el código, no la base.

## `usuario`

Cómo entra alguien al sistema.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `persona_id` | bigint | sí | A qué persona pertenece esta cuenta |
| `correo` | text | no | Con el que entra. Se vacía al anonimizar |
| `contrasena_hash` | text | sí | La contraseña cifrada de forma irreversible |
| `area_id` | bigint | no | Solo el equipo interno. Los candidatos no tienen área |
| `es_activo` | boolean | sí | Si puede entrar. Por defecto `true` |
| `ultimo_acceso_en` | timestamptz | no | |
| `creado_en` | timestamptz | sí | |

**Clave primaria:** `id`
**Foráneas:** `persona_id` → `persona`, `area_id` → `area`
**Único:** `persona_id` (una persona, una cuenta) · `lower(correo)` (dos personas no comparten correo)

El índice único va sobre `lower(correo)` para que `Ana@renaser.pe` y `ana@renaser.pe` sean el
mismo correo.

## `area`

El departamento que contrata.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `nombre` | text | sí | Operaciones, Marketing, Talento… |
| `es_activa` | boolean | sí | |

**Clave primaria:** `id` · **Único:** `nombre`

## `rol`

Un nombre y una lista de permisos.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(40) | sí | `CANDIDATO`, `RECLUTADOR`, `JEFE_AREA`, `DIRECCION` |
| `nombre` | text | sí | Como se ve en pantalla |
| `descripcion` | text | no | Para qué sirve este rol |
| `es_sistema` | boolean | sí | Los cuatro iniciales no se pueden borrar |

**Clave primaria:** `id` · **Único:** `codigo`

## `permiso`

Una acción suelta que se concede o no. Son 53.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(60) | sí | `vacante.cerrar`, `nota.ajustar` |
| `etiqueta` | text | sí | «Puede cerrar una vacante». Es lo que se ve en pantalla |
| `grupo` | varchar(40) | sí | Vacantes, Candidatos, Evaluación, Configuración… |
| `descripcion` | text | no | Qué implica, en lenguaje normal |
| `orden` | smallint | sí | Para presentarlos siempre igual |

**Clave primaria:** `id` · **Único:** `codigo`

La pantalla donde Dirección reparte permisos muestra `etiqueta`, nunca `codigo`.

## `usuario_rol`

Una persona puede tener varios roles y hace lo que le permita cualquiera de ellos.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `usuario_id` | bigint | sí | Quién |
| `rol_id` | bigint | sí | Qué rol |
| `asignado_por_usuario_id` | bigint | no | Quién se lo dio |
| `asignado_en` | timestamptz | sí | |

**Clave primaria:** `usuario_id` + `rol_id`

## `rol_permiso`

Qué permisos tiene un rol y **con qué alcance**.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `rol_id` | bigint | sí | |
| `permiso_id` | bigint | sí | |
| `alcance` | varchar(20) | sí | `PROPIO`, `SUS_VACANTES` o `TODO` |

**Clave primaria:** `rol_id` + `permiso_id`

El alcance es lo que distingue «ve candidatos» de «ve **sus** candidatos». Sin esta columna,
el jefe del área vería los de toda la empresa.

---

# 2 · Consentimiento y borrado

## `texto_consentimiento`

El texto que el candidato acepta, versionado.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `version` | varchar(20) | sí | `2026.1` |
| `texto` | text | sí | El texto completo tal como se mostró |
| `estado` | varchar(20) | sí | `BORRADOR` o `PUBLICADA` |
| `publicado_por_usuario_id` | bigint | no | |
| `publicado_en` | timestamptz | no | |

**Clave primaria:** `id` · **Único:** `version`

Una versión publicada **no se modifica nunca**. Corregir una coma significa publicar otra.

## `consentimiento`

Que esta persona aceptó esta versión concreta.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `persona_id` | bigint | sí | Quién aceptó |
| `texto_consentimiento_id` | bigint | sí | Qué versión firmó |
| `aceptado_en` | timestamptz | sí | Fecha y hora exactas |
| `ip` | inet | sí | Desde dónde |

**Clave primaria:** `id` · **Único:** `persona_id` + `texto_consentimiento_id`

Guardar la versión, y no un simple sí, es lo que exige la ley 29733: hay que poder demostrar
qué texto tenía delante esa persona ese día.

## `solicitud_borrado`

Pedir el borrado y ejecutarlo son dos cosas distintas, con días de por medio.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `persona_id` | bigint | sí | Quién lo pide |
| `motivo` | text | no | Si lo quiso dar |
| `solicitado_en` | timestamptz | sí | |
| `ejecutado_en` | timestamptz | no | Vacío mientras esté pendiente |
| `ejecutado_por_usuario_id` | bigint | no | Solo Dirección puede |

**Clave primaria:** `id`

---

# 3 · Vacantes

## `nivel_puesto`

Los tres niveles. Deciden cuántas preguntas se responden y cuánto pesa cada etapa.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(20) | sí | `DIRECCION`, `SUPERVISION`, `EJECUCION` |
| `nombre` | text | sí | Como se ve en pantalla |
| `preguntas_banco` | smallint | sí | 90, 60 o 50 |
| `orden` | smallint | sí | |

**Clave primaria:** `codigo` · **Filas iniciales:** 3

## `puesto`

El catálogo de puestos de Renaser.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(40) | sí | `DESARROLLADOR_WEB` |
| `nombre` | text | sí | «Desarrollador web» |
| `nivel_puesto_codigo` | varchar(20) | sí | A qué nivel pertenece |
| `es_activo` | boolean | sí | |

**Clave primaria:** `id` · **Único:** `codigo`

⚠️ Los documentos del cliente usan dos juegos de nombres para los mismos puestos. Hay que
fijar los definitivos antes de cargar esta tabla.

## `vacante`

Una convocatoria concreta.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `uuid` | uuid | sí | Identificador público, el que va en la dirección web |
| `puesto_id` | bigint | sí | |
| `titulo` | text | sí | Como aparece en el portal |
| `descripcion` | text | sí | |
| `requisitos` | text | no | |
| `estado` | varchar(20) | sí | `BORRADOR`, `ABIERTA`, `CERRADA` |
| `nota_minima` | numeric(5,2) | no | Debajo de esto no se avanza |
| `version_plantilla_prueba_id` | bigint | no | Qué prueba se aplica |
| `jefe_usuario_id` | bigint | sí | El jefe del área que contrata |
| `creada_por_usuario_id` | bigint | sí | |
| `publicada_en` | timestamptz | no | |
| `cerrada_en` | timestamptz | no | |

**Clave primaria:** `id` · **Único:** `uuid`

El reclutador crea y publica sin que nadie apruebe. Al cerrarla, las postulaciones a mitad se
cierran también y se avisa a esas personas.

## `bar_raiser_asignacion`

Quién revisa como Bar Raiser en esta vacante. Es una función por vacante, no un rol.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `vacante_id` | bigint | sí | |
| `usuario_id` | bigint | sí | Alguien de Renaser ajeno al área que contrata |
| `asignado_por_usuario_id` | bigint | sí | |
| `asignado_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `vacante_id` + `usuario_id`

El sistema no deja nombrar Bar Raiser al jefe del área de esa vacante. Esa comprobación es del
código: hay que mirar `vacante.jefe_usuario_id` en la misma operación.

---

# 4 · Postulación y su historia

## `estado_postulacion`

Catálogo cerrado de los 25 estados. Solo cambia con una migración.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(40) | sí | `CV_EN_REVISION`, `PRUEBA_EN_CURSO`… |
| `nombre` | text | sí | Como se ve en pantalla |
| `espera_a` | varchar(20) | sí | `CANDIDATO`, `SISTEMA`, `RECLUTADOR`, `JEFE_AREA`, `NADIE` |
| `es_final` | boolean | sí | Contratado, no continúa y cerrada |
| `etapa_codigo` | varchar(20) | no | A qué etapa del embudo pertenece |
| `orden` | smallint | sí | |

**Clave primaria:** `codigo` · **Filas iniciales:** 25

`espera_a` es la columna que arma la bandeja de trabajo del reclutador: «muéstrame todo lo que
está esperándome a mí».

## `postulacion`

Un usuario en una vacante. Tiene un solo estado a la vez, nunca dos.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `uuid` | uuid | sí | Identificador público |
| `usuario_id` | bigint | sí | Quién postula |
| `vacante_id` | bigint | sí | A qué |
| `estado_codigo` | varchar(40) | sí | En qué estado está ahora |
| `motivo_cierre` | varchar(40) | no | Solo si el estado es final |
| `evaluacion_id` | bigint | no | Qué evaluación se le está contando |
| `pruebas_adicionales_usadas` | smallint | sí | Cuántas veces salió ámbar. Empieza en 0 |
| `movido_en` | timestamptz | sí | Última vez que cambió de estado |
| `creada_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `uuid` · **Único:** `usuario_id` + `vacante_id`

Motivos de `NO_CONTINUA`: nota debajo del mínimo · fallo grave confirmado · decisión roja ·
decisión de una persona.
Motivos de `CERRADA`: se cerró la convocatoria · sin avanzar el plazo configurado · cierre
manual · el candidato se retiró · no fue a la simulación · pidió borrar sus datos.

`movido_en` es lo que alimenta el «lleva 14 días sin avanzar» del panel.

## `transicion_estado`

Cada cambio de estado, guardado aparte. **No se modifica ni se borra nunca.**

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `estado_anterior_codigo` | varchar(40) | no | Vacío en la primera transición |
| `estado_nuevo_codigo` | varchar(40) | sí | |
| `usuario_id` | bigint | no | Vacío cuando lo hizo el sistema |
| `rol_id` | bigint | no | Con qué rol actuaba esa persona |
| `es_sistema` | boolean | sí | Distingue a la máquina de una persona |
| `motivo` | text | no | Obligatorio cuando `es_sistema` es falso |
| `ocurrida_en` | timestamptz | sí | |

**Clave primaria:** `id`

Esta tabla es la que permite reconstruir el recorrido de cualquier candidato, calcular cuánto
se tarda en cada etapa y medir qué porcentaje de decisiones tomó la máquina sola. Por eso
`es_sistema` es columna propia y no se deduce de que `usuario_id` esté vacío.

La base impide actualizarla y borrarla con un disparador, no solo con permisos.

---

# 5 · Criterios y notas

Cuatro etapas puntúan al candidato repartiendo 100 puntos entre varios criterios: el CV entre
ocho, la prueba del puesto entre los que defina cada plantilla, la simulación entre diez y la
validación de 7 días entre nueve métricas.

Antes eran ocho tablas: un catálogo y una tabla de notas por cada etapa, con las mismas
columnas repetidas cuatro veces. Ahora son **dos**. Si mañana hay que añadir «quién revisó esta
nota», se toca en un sitio y no en cuatro.

## `criterio`

Cualquier cosa que se puntúa, de la etapa que sea.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(40) | sí | `RESULTADOS_DEMOSTRABLES`, `PRIORIZACION`, `CONFIABILIDAD` |
| `nombre` | text | sí | «Resultados demostrables» |
| `etapa_codigo` | varchar(20) | sí | `CV`, `PRUEBA_PUESTO`, `SIMULACION`, `VALIDACION` |
| `version_plantilla_prueba_id` | bigint | no | Solo los de la prueba del puesto |
| `puntos` | smallint | no | Solo los de la prueba. Los demás tienen su peso versionado |
| `orden` | smallint | sí | |
| `es_activo` | boolean | sí | Se retira sin borrarlo |

**Clave primaria:** `id`
**Único:** `etapa_codigo` + `codigo` cuando `version_plantilla_prueba_id` está vacío
**Único:** `version_plantilla_prueba_id` + `codigo` cuando no lo está

**Filas iniciales:** 27 globales — 8 del CV, 10 de la simulación y 9 de la validación —, más
las que traiga cada plantilla de prueba.

Hay dos clases de criterio y por eso hay dos columnas que a veces quedan vacías:

- **Los globales** (CV, simulación, validación) son iguales para toda la empresa. Su peso vive
  en `peso_criterio`, dentro de una versión de pesos, porque Dirección los cambia y no pueden
  cambiar el pasado.
- **Los de la prueba del puesto** pertenecen a una versión de plantilla concreta y cambian
  según el puesto: la prueba de desarrollador reparte los 100 puntos distinto que la de
  ventas. Sus `puntos` van en la propia fila, porque la versión de plantilla **ya está
  congelada**: no hace falta versionarlos otra vez.

## `nota_criterio`

El puntaje de un criterio para una postulación, y por qué.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | De quién es la nota |
| `criterio_id` | bigint | sí | Qué se puntuó |
| `puntaje` | numeric(5,2) | no | Vacío mientras esté pendiente |
| `explicacion` | text | no | Obligatoria si hay puntaje |
| `ejecucion_ia_id` | bigint | no | Si la puso la máquina, qué intento la produjo |
| `calificada_por_usuario_id` | bigint | no | Si la puso una persona |
| `ajustada_por_usuario_id` | bigint | no | Si alguien cambió la nota original |
| `motivo_ajuste` | text | no | Obligatorio si hay ajuste |
| `registrada_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `postulacion_id` + `criterio_id`

Todas las notas de criterio cuelgan de la **postulación**, y eso vale para las cuatro etapas:
el CV, la prueba y la validación son una por postulación, y de la simulación solo cuenta la
sesión vigente.

`ejecucion_ia_id` y `calificada_por_usuario_id` se excluyen entre sí: o la puso la máquina o
la puso una persona. En la **simulación y la validación siempre es una persona** —la máquina
no califica esas dos etapas—, así que ahí `ejecucion_ia_id` está siempre vacío.

El puntaje admite quedar vacío porque **si la máquina falla, la nota queda pendiente y se
reintenta**. Nunca se guarda un cero por un problema técnico.

---

# 6 · El CV

## `archivo`

Los archivos viven fuera de la base. Aquí solo está su ruta.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `ruta` | text | sí | Dónde está en el almacén |
| `nombre_original` | text | sí | Como lo llamó quien lo subió |
| `tamano_bytes` | bigint | sí | Hasta 200 MB |
| `tipo_mime` | varchar(120) | sí | |
| `subido_en` | timestamptz | sí | |
| `borrado_en` | timestamptz | no | Se marca al ejecutar un borrado de datos |

**Clave primaria:** `id`

El almacén es privado. Para abrir un archivo, el backend genera un enlace firmado que dura
poco.

## `cv`

El currículum de una postulación, en sus dos versiones.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `archivo_original_id` | bigint | sí | El que sube el candidato. Lo ve el reclutador |
| `archivo_anonimizado_id` | bigint | no | Sin foto, edad, sexo ni estado civil |
| `subido_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `postulacion_id`

Son dos archivos porque la versión anonimizada es **la única que se le envía a la máquina**, y
hay que poder demostrar que la regla se cumplió.

## `enlace_cv`

Portafolio, repositorio, proyectos.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `cv_id` | bigint | sí | |
| `url` | text | sí | |
| `tipo` | varchar(30) | no | `PORTAFOLIO`, `REPOSITORIO`, `OTRO` |

**Clave primaria:** `id`

## `afirmacion_no_verificada`

Algo que el CV dice sin respaldo.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `cv_id` | bigint | sí | |
| `texto` | text | sí | Lo que dice el CV |
| `estado` | varchar(20) | sí | `PENDIENTE`, `VERIFICADA`, `DESCARTADA` |
| `ejecucion_ia_id` | bigint | no | |

**Clave primaria:** `id`

No es una mentira: es algo que hace falta repreguntar. Por eso tiene tres estados y no es un
sí o un no.

---

# 7 · Banco de preguntas

## `dimension`

Las 22 cosas que se miden.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(10) | sí | `INT`, `CRI`, `QUA`, `FIT`… |
| `nombre` | text | sí | «Integridad y verdad» |
| `definicion` | text | sí | Qué significa exactamente |

**Clave primaria:** `codigo` · **Filas iniciales:** 22

Son 22 y no 18: hay cuatro (`POT`, `SELF`, `VAL`, `FIT`) que se usan en preguntas reales pero
solo están definidas en un documento del cliente distinto del principal.

El candidato **nunca** ve estos nombres.

## `conjunto_dimension`

Algunos pesos se aplican a un par de dimensiones juntas, no a cada una.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(20) | sí | `SUP_PPL`, `PRI_DEC`, `VEL_LRN` |
| `nombre` | text | sí | |

**Clave primaria:** `id` · **Único:** `codigo`

Un conjunto puede tener una sola dimensión. Así todos los pesos se expresan igual.

## `conjunto_dimension_miembro`

Qué dimensiones forman ese conjunto.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `conjunto_dimension_id` | bigint | sí | |
| `dimension_codigo` | varchar(10) | sí | |

**Clave primaria:** `conjunto_dimension_id` + `dimension_codigo`

## `version_banco`

Una versión del banco, en borrador o publicada.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `tipo_banco` | varchar(20) | sí | `NIVEL` o `ALINEACION` |
| `nivel_puesto_codigo` | varchar(20) | no | Vacío cuando es de alineación |
| `etiqueta` | varchar(40) | sí | `DIR_V1_2026-08` |
| `estado` | varchar(20) | sí | `BORRADOR`, `PUBLICADA`, `RETIRADA` |
| `publicada_por_usuario_id` | bigint | no | Solo Dirección |
| `publicada_en` | timestamptz | no | |
| `creada_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `etiqueta`

El reclutador prepara el borrador; **publicar es acto exclusivo de Dirección**. Una versión
publicada no se modifica: editar crea otra.

`tipo_banco` existe porque **no se puede deducir del código de la pregunta**: las de
autogestión se llaman C01 a C12, pero el «banco C» es el de Ejecución, cuyas preguntas son O01
a O50.

## `pregunta`

Una pregunta dentro de una versión.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `version_banco_id` | bigint | sí | |
| `codigo` | varchar(10) | sí | `D01`, `S23`, `O47`, `M02` |
| `bloque` | varchar(10) | no | `A1` a `A6`, o `DINERO`/`CONFLICTO`/`AUTOGESTION` |
| `tipo` | varchar(20) | sí | `ESTILO`, `SITUACION`, `CONDUCTUAL`, `MICROCASO`, `DILEMA`, `CONSISTENCIA` |
| `enunciado` | text | sí | La pregunta |
| `situacion` | text | no | El escenario, en las de situación |
| `logica_interna` | text | no | Qué se espera. Solo en las que no tienen clave numérica |
| `disyuntiva` | varchar(60) | no | `VEL frente a CRI`. Solo en las de estilo |
| `es_puntuable` | boolean | sí | Falso en estilo y consistencia |
| `orden` | smallint | sí | |

**Clave primaria:** `id` · **Único:** `version_banco_id` + `codigo`

`es_puntuable` en falso significa dos cosas distintas según el tipo: las de **estilo** solo
dibujan el perfil de la persona; las de **consistencia** generan alertas comparándolas con
otras respuestas.

## `opcion`

Las opciones de respuesta.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `pregunta_id` | bigint | sí | |
| `letra` | char(1) | sí | `A`, `B`, `C`, `D` |
| `texto` | text | sí | |
| `puntaje` | smallint | no | De 0 a 4. **Vacío es normal** |

**Clave primaria:** `id` · **Único:** `pregunta_id` + `letra`

El puntaje queda vacío en dos casos reales: las preguntas de estilo, que no tienen respuesta
correcta y reparten puntos por dimensión; y las situacionales donde el documento del cliente
solo definió la opción buena (`B=4`) y dejó las demás sin valor.

## `opcion_dimension`

Cuánto suma cada opción a cada dimensión.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `opcion_id` | bigint | sí | |
| `dimension_codigo` | varchar(10) | sí | |
| `incremento` | smallint | sí | Normalmente 2 al principal y 1 al secundario |

**Clave primaria:** `opcion_id` + `dimension_codigo`

Esta es la tabla que hace falta para las preguntas de estilo: elegir A suma 2 a *velocidad con
criterio* y 1 a *iniciativa*. No cabe en una columna de la opción.

## `pregunta_dimension`

Qué dimensiones evalúa una pregunta abierta, que no tiene opciones.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `pregunta_id` | bigint | sí | |
| `dimension_codigo` | varchar(10) | sí | |
| `es_principal` | boolean | sí | Distingue la dimensión central de las secundarias |

**Clave primaria:** `pregunta_id` + `dimension_codigo`

## `par_consistencia`

Dos preguntas que miden lo mismo y deberían responderse parecido.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `version_banco_id` | bigint | sí | |
| `pregunta_a_id` | bigint | sí | |
| `pregunta_b_id` | bigint | sí | |
| `diferencia_maxima` | smallint | sí | Más que esto genera una alerta |

**Clave primaria:** `id` · **Único:** `pregunta_a_id` + `pregunta_b_id`

⚠️ Los documentos del cliente dicen que estos pares existen, pero nunca dicen cuáles con
cuáles. La tabla arranca vacía.

---

# 8 · Evaluación

## `evaluacion`

Las respuestas de un usuario a un nivel concreto. **Cuelga del usuario, no de la postulación.**

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `usuario_id` | bigint | sí | De quién son las respuestas |
| `version_banco_nivel_id` | bigint | sí | Qué banco de nivel respondió |
| `version_banco_alineacion_id` | bigint | sí | Qué versión de las 36 de alineación |
| `estado` | varchar(20) | sí | `EN_CURSO`, `TERMINADA` |
| `iniciada_en` | timestamptz | sí | |
| `terminada_en` | timestamptz | no | |

**Clave primaria:** `id` · **Único:** `usuario_id` + `version_banco_nivel_id`

Esta es la tabla que permite que quien postula a otro puesto del mismo nivel **no repita
noventa preguntas**. Si colgara de la postulación habría que copiarlas cada vez.

## `orden_pregunta`

En qué orden se le mostró cada pregunta y sus opciones.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `evaluacion_id` | bigint | sí | |
| `pregunta_id` | bigint | sí | |
| `posicion` | smallint | sí | En qué lugar salió |
| `orden_opciones` | varchar(20) | sí | `C,A,D,B` |

**Clave primaria:** `id` · **Único:** `evaluacion_id` + `pregunta_id`

Sin esta tabla no se puede cumplir la promesa de reproducir cualquier evaluación pasada tal
como se vio. El orden es aleatorio y distinto para cada persona, así que hay que guardarlo.

## `respuesta`

Lo que contestó.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `evaluacion_id` | bigint | sí | |
| `pregunta_id` | bigint | sí | |
| `opcion_id` | bigint | no | En las de opción múltiple |
| `texto` | text | no | En las abiertas. Se vacía al anonimizar |
| `segundos` | integer | no | Cuánto tardó en esa pregunta |
| `respondida_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `evaluacion_id` + `pregunta_id`

Cada respuesta se guarda al momento, así que si se corta la luz retoma donde quedó. Una vez
enviada **no puede volver atrás a cambiarla**.

## `nota_respuesta`

El puntaje de esa respuesta y por qué.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `respuesta_id` | bigint | sí | Clave y a la vez foránea |
| `puntaje` | numeric(4,2) | no | De 0 a 4 |
| `explicacion` | text | no | Obligatoria si hay puntaje |
| `ejecucion_ia_id` | bigint | no | |
| `ajustada_por_usuario_id` | bigint | no | |
| `motivo_ajuste` | text | no | |
| `ajustada_en` | timestamptz | no | |

**Clave primaria:** `respuesta_id`

Las de opción múltiple se corrigen solas contra la clave. Las abiertas las califica la máquina
con la guía de 0 a 4 y **guarda su explicación**.

## `resultado_alineacion`

El semáforo de cada uno de los tres bloques.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `evaluacion_id` | bigint | sí | |
| `bloque` | varchar(20) | sí | `DINERO`, `CONFLICTO`, `AUTOGESTION` |
| `puntaje` | numeric(5,2) | no | |
| `semaforo` | varchar(10) | sí | `VERDE`, `AMBAR`, `ROJO` |

**Clave primaria:** `evaluacion_id` + `bloque`

Son tres valores, no cuatro: aquí no existe «sin datos». Y un **rojo no descarta a nadie**:
genera preguntas para la conversación final.

## `alerta`

Contradicciones y respuestas demasiado ideales.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `tipo` | varchar(30) | sí | `CONTRADICCION`, `RESPUESTA_IDEAL`, `ALINEACION_ROJA` |
| `descripcion` | text | sí | Qué se detectó, en lenguaje normal |
| `pregunta_a_id` | bigint | no | Las preguntas implicadas |
| `pregunta_b_id` | bigint | no | |
| `ejecucion_ia_id` | bigint | no | |
| `confirmada_por_usuario_id` | bigint | no | |
| `confirmada_en` | timestamptz | no | |
| `creada_en` | timestamptz | sí | |

**Clave primaria:** `id`

Una alerta **nunca descarta**. Queda visible en la ficha y se convierte en preguntas para la
conversación final.

---

# 9 · Prueba del puesto

## `plantilla_prueba`

La prueba de un puesto.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `puesto_id` | bigint | sí | |
| `nombre` | text | sí | |
| `es_activa` | boolean | sí | |

**Clave primaria:** `id`

## `version_plantilla_prueba`

Una versión concreta. Si tiene vacante, es una copia privada de esa vacante.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `plantilla_prueba_id` | bigint | sí | |
| `vacante_id` | bigint | no | Si está, esta versión es solo de esa vacante |
| `etiqueta` | varchar(40) | sí | |
| `enunciado` | text | sí | Lo que tiene que hacer |
| `entregables_esperados` | text | no | Qué debe entregar |
| `duracion_minutos` | smallint | sí | 60, 75, 90 o 120 |
| `minuto_cambio` | smallint | no | Cuándo aparece el cambio inesperado |
| `minutos_extra` | smallint | no | Cuánto tiene para adaptarse |
| `texto_cambio` | text | no | Qué dice el cambio |
| `estado` | varchar(20) | sí | `BORRADOR`, `PUBLICADA`, `RETIRADA` |
| `publicada_por_usuario_id` | bigint | no | |
| `publicada_en` | timestamptz | no | |

**Clave primaria:** `id`

Al crear una vacante se elige una plantilla y **se puede modificar solo para esa vacante** sin
tocar la original. Eso es lo que hace `vacante_id`.

Los cambios en una prueba **no afectan a quien ya la rindió**: cada intento queda atado a su
versión.

## `intento_prueba`

Cuando un candidato rinde.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `version_plantilla_prueba_id` | bigint | sí | La versión congelada con que rindió |
| `iniciado_en` | timestamptz | sí | Desde aquí corre el reloj |
| `entregado_en` | timestamptz | no | |
| `es_entrega_automatica` | boolean | sí | Si lo entregó el reloj por él |
| `cambio_mostrado_en` | timestamptz | no | Cuándo apareció el cambio inesperado |

**Clave primaria:** `id` · **Único:** `postulacion_id`

El reloj lo lleva el servidor. Si cierra la página, el tiempo sigue corriendo. Lo que queda se
calcula desde `iniciado_en`: **no hay acumulador de pausas porque no se puede pausar**.

Cuando se acaba, el sistema entrega solo. **No existe entregar tarde**, y por eso hay una
marca en vez de un estado.

## `entregable`

Lo que sube o el enlace que pega.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `intento_prueba_id` | bigint | sí | |
| `archivo_id` | bigint | no | Si subió un archivo |
| `enlace` | text | no | Si pegó una dirección (Figma, repositorio, Drive) |
| `subido_en` | timestamptz | sí | |

**Clave primaria:** `id`

Uno de los dos, archivo o enlace, tiene que estar. Eso lo comprueba el código.

## `pregunta_autoevaluacion`

Las 17 preguntas que responde todo el mundo después de entregar.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `codigo` | varchar(10) | sí | |
| `enunciado` | text | sí | |
| `orden` | smallint | sí | |

**Clave primaria:** `id` · **Único:** `codigo` · **Filas iniciales:** 17

Son iguales para toda prueba, de cualquier puesto.

## `respuesta_autoevaluacion`

Sus respuestas.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `intento_prueba_id` | bigint | sí | |
| `pregunta_autoevaluacion_id` | bigint | sí | |
| `texto` | text | no | Se vacía al anonimizar |

**Clave primaria:** `intento_prueba_id` + `pregunta_autoevaluacion_id`

Aquí es donde el candidato dice **qué parte hizo con inteligencia artificial y qué verificó
él**. Eso vale 5 de los 100 puntos. El sistema no usa detectores: pregunta.

> Los criterios con que se califica el entregable y sus notas están en «Criterios y notas».
> El reparto de puntos de cada prueba vive en `criterio`, atado a la versión de plantilla.
> La suma **no está obligada a dar 100**: el sistema avisa si no cuadra, pero deja guardar.

---

# 10 · Simulación de 2 horas

## `sesion_simulacion`

Una fecha con cupo. No hay límite de cuántas se crean.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `fecha_hora` | timestamptz | sí | Cuándo |
| `lugar` | text | no | Dónde |
| `cupo` | smallint | sí | Cuánta gente cabe. Se puede ampliar después |
| `estado` | varchar(20) | sí | `ABIERTA`, `LLENA`, `CANCELADA`, `REALIZADA` |
| `creada_por_usuario_id` | bigint | sí | |
| `cancelada_en` | timestamptz | no | |

**Clave primaria:** `id`

Cuando se llena deja de ofrecerse. Si se cancela, a los inscritos se les avisa y vuelven a
quedar pendientes de elegir otra.

## `sesion_vacante`

Para qué vacantes sirve esa sesión: una, varias o todas.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `sesion_simulacion_id` | bigint | sí | |
| `vacante_id` | bigint | sí | |

**Clave primaria:** `sesion_simulacion_id` + `vacante_id`

El candidato solo ve las sesiones de su vacante que tengan cupo. Si no hay ninguna, su
postulación aparece en la bandeja del reclutador.

## `inscripcion_sesion`

El candidato eligió esta fecha.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `sesion_simulacion_id` | bigint | sí | |
| `postulacion_id` | bigint | sí | |
| `inscrita_en` | timestamptz | sí | |
| `asistio` | boolean | no | Vacío hasta que alguien lo marca |
| `marcada_por_usuario_id` | bigint | no | |
| `es_vigente` | boolean | sí | Falso si la sesión se canceló y eligió otra |

**Clave primaria:** `id`
**Único parcial:** `postulacion_id` cuando `es_vigente` es verdadero

`es_vigente` permite guardar el historial: si su primera sesión se canceló, esa inscripción se
queda como estaba y se crea otra. Así se sabe que le cancelaron una fecha.

## `momento_simulacion`

Los seis tramos de las dos horas.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(20) | sí | `CONTEXTO`, `PREGUNTAS`, `EJECUCION`, `CAMBIO`, `ENTREGA`, `CONVERSACION` |
| `nombre` | text | sí | |
| `minuto_inicio` | smallint | sí | |
| `minuto_fin` | smallint | sí | |

**Clave primaria:** `codigo` · **Filas iniciales:** 6

## `marca_tiempo_simulacion`

Diez momentos que el sistema anota solo.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `inscripcion_sesion_id` | bigint | sí | |
| `evento` | varchar(40) | sí | `PRIMERA_PREGUNTA`, `INICIO_TRABAJO`, `REACCION_AL_CAMBIO`… |
| `ocurrida_en` | timestamptz | sí | Con precisión de segundos |

**Clave primaria:** `id` · **Único:** `inscripcion_sesion_id` + `evento`

De estas marcas salen las cinco preguntas de la conversación final: «a las 11:42 detectaste el
bloqueo y lo avisaste a las 12:10, ¿qué pasó en esa media hora?».

> Los diez aspectos que se califican y sus notas están en «Criterios y notas», con
> `etapa_codigo = SIMULACION`. **Los pone una persona, nunca la máquina.**

## `pregunta_generada`

Las cinco preguntas personalizadas para la conversación final.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `texto` | text | sí | |
| `alerta_id` | bigint | no | De qué contradicción salió |
| `ejecucion_ia_id` | bigint | no | |
| `orden` | smallint | sí | |

**Clave primaria:** `id`

Salen de las contradicciones detectadas **en toda la evaluación**, no solo en la simulación.

---

# 11 · Validación de 7 días y decisión

## `validacion`

El periodo de trabajo real.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `inicio_en` | date | sí | |
| `fin_en` | date | no | |
| `estado` | varchar(20) | sí | `EN_CURSO`, `TERMINADA`, `INTERRUMPIDA` |
| `iniciada_por_usuario_id` | bigint | sí | |

**Clave primaria:** `id` · **Único:** `postulacion_id`

⚠️ La figura legal de este periodo sigue **sin definir**, y eso bloquea la etapa, no el
modelo.

> Las nueve métricas y sus puntajes están en «Criterios y notas», con
> `etapa_codigo = VALIDACION`. Se cargan a mano, y quién puede hacerlo es configurable: solo
> el jefe del área, solo el reclutador, o ambos. Arranca con ambos habilitados.

## `etapa`

Las cinco etapas del embudo.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(20) | sí | `CV`, `EVALUACION`, `PRUEBA_PUESTO`, `SIMULACION`, `VALIDACION` |
| `nombre` | text | sí | |
| `orden` | smallint | sí | |

**Clave primaria:** `codigo` · **Filas iniciales:** 5

## `nota_etapa`

La nota de cada etapa, atada a la versión de pesos con que se calculó.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `postulacion_id` | bigint | sí | |
| `etapa_codigo` | varchar(20) | sí | |
| `puntaje` | numeric(5,2) | no | |
| `version_pesos_id` | bigint | sí | Con qué pesos se calculó |
| `calculada_en` | timestamptz | sí | |

**Clave primaria:** `postulacion_id` + `etapa_codigo`

`version_pesos_id` es lo que hace que **la nota no cambie sola**. Si mañana Dirección cambia
los pesos, se publica otra versión y esta fila sigue apuntando a la de antes.

## `decision`

El semáforo final.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `semaforo` | varchar(15) | sí | `VERDE`, `AMBAR`, `ROJO`, `SIN_DATOS` |
| `nota_global` | numeric(5,2) | no | |
| `version_pesos_id` | bigint | sí | |
| `cobertura_evidencia` | numeric(5,2) | no | Cuánta evidencia se llegó a reunir |
| `decidida_por_usuario_id` | bigint | no | El jefe del área o Dirección. Nunca el reclutador |
| `motivo` | text | no | |
| `decidida_en` | timestamptz | no | |

**Clave primaria:** `id` · **Único:** `postulacion_id`

**No es un promedio.** `SIN_DATOS` es distinto de `ROJO`: significa que falta evidencia, no
que la persona falle.

## `tipo_fallo_grave`

Qué no se perdona en cada nivel. Configurable.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `nivel_puesto_codigo` | varchar(20) | sí | |
| `descripcion` | text | sí | «No sabe priorizar», «Esconde bloqueos» |
| `es_activo` | boolean | sí | Se puede desactivar sin borrarlo |

**Clave primaria:** `id`

## `fallo_grave`

Uno detectado en un candidato concreto.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `tipo_fallo_grave_id` | bigint | sí | |
| `explicacion` | text | sí | En qué se basa |
| `ejecucion_ia_id` | bigint | no | Si lo detectó la máquina |
| `confirmado_por_usuario_id` | bigint | no | **Vacío significa que aún no bloquea** |
| `confirmado_en` | timestamptz | no | |

**Clave primaria:** `id`

La máquina lo detecta y lo explica, pero **siempre lo confirma una persona antes de que
bloquee a nadie**. Mientras `confirmado_por_usuario_id` esté vacío, es solo una sospecha
anotada.

## `opinion_bar_raiser`

Su revisión escrita.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `usuario_id` | bigint | sí | |
| `texto` | text | sí | |
| `registrada_en` | timestamptz | sí | |

**Clave primaria:** `id`

**Solo opina.** No hay columna de veredicto ni de bloqueo porque no puede bloquear una
contratación. Su opinión se muestra junto a la decisión final.

## `prueba_adicional`

La prueba que se pide cuando sale ámbar.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `numero` | smallint | sí | 1 o 2. El tope es configurable |
| `motivo` | text | sí | Qué contradicción se quiere resolver |
| `enunciado` | text | sí | |
| `solicitada_por_usuario_id` | bigint | sí | |
| `solicitada_en` | timestamptz | sí | |
| `entregada_en` | timestamptz | no | |
| `puntaje` | numeric(5,2) | no | |

**Clave primaria:** `id` · **Único:** `postulacion_id` + `numero`

Al llegar al tope el sistema ya no permite otra y obliga a decidir verde o rojo con lo que
hay. El tope está en `parametro`, así que la comprobación es del código.

---

# 12 · Configuración

## `version_pesos`

Una versión de todos los pesos, en borrador o publicada.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `etiqueta` | varchar(40) | sí | `PESOS_V1_2026-08` |
| `estado` | varchar(20) | sí | `BORRADOR`, `PUBLICADA`, `RETIRADA` |
| `publicada_por_usuario_id` | bigint | no | Solo Dirección |
| `publicada_en` | timestamptz | no | |
| `creada_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `etiqueta`

Una versión agrupa las cuatro tablas de pesos que siguen. Publicarla las congela juntas: no
tiene sentido cambiar el peso de una etapa sin mirar las demás.

## `peso_etapa`

Cuánto pesa cada etapa en cada nivel.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `version_pesos_id` | bigint | sí | |
| `nivel_puesto_codigo` | varchar(20) | sí | |
| `etapa_codigo` | varchar(20) | sí | |
| `peso` | numeric(5,2) | sí | |

**Clave primaria:** `version_pesos_id` + `nivel_puesto_codigo` + `etapa_codigo`

Quince filas por versión: cinco etapas por tres niveles.

## `peso_componente_evaluacion`

Cómo se reparte la evaluación integral por dentro.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `version_pesos_id` | bigint | sí | |
| `componente` | varchar(20) | sí | `PSICOMETRIA`, `BANCO`, `ALINEACION` |
| `peso` | numeric(5,2) | sí | |
| `es_activo` | boolean | sí | La psicométrica arranca inactiva |

**Clave primaria:** `version_pesos_id` + `componente`

La psicométrica todavía no se ha comprado. Mientras `es_activo` sea falso, su 30% se reparte
entre las otras dos. El día que exista se activa **sin rehacer nada**.

## `peso_dimension`

Cuánto pesa cada conjunto de dimensiones en cada nivel.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `version_pesos_id` | bigint | sí | |
| `nivel_puesto_codigo` | varchar(20) | sí | |
| `conjunto_dimension_id` | bigint | sí | |
| `peso` | numeric(5,2) | sí | |

**Clave primaria:** `version_pesos_id` + `nivel_puesto_codigo` + `conjunto_dimension_id`

## `peso_criterio`

Cuánto vale cada criterio en cada nivel. Cubre las tres etapas de criterios globales: CV,
simulación y validación.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `version_pesos_id` | bigint | sí | |
| `nivel_puesto_codigo` | varchar(20) | sí | |
| `criterio_id` | bigint | sí | |
| `peso` | numeric(5,2) | sí | |

**Clave primaria:** `version_pesos_id` + `nivel_puesto_codigo` + `criterio_id`

Admite **peso cero explícito**: «Desarrollo de personas» vale 0 en Ejecución. Cero no es lo
mismo que no estar.

Los criterios del CV sí varían por nivel; los de la simulación y la validación, hoy no. Aun
así **se repite la fila en los tres niveles con el mismo valor**, en vez de dejar el nivel
vacío. Son 27 criterios por 3 niveles: 81 filas por versión, nada. A cambio se gana que el día
que el cliente decida que la confiabilidad pesa distinto en Dirección que en Ejecución, no hay
que tocar el esquema.

Los criterios de la prueba del puesto **no entran aquí**: sus puntos viven en `criterio`,
atados a una versión de plantilla que ya está congelada.

## `parametro`

Los valores sueltos que el cliente cambia.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(60) | sí | `ZONA_DUDOSA_MARGEN`, `DIAS_SIN_AVANZAR` |
| `valor` | text | sí | Guardado como texto, se convierte al leer |
| `tipo` | varchar(20) | sí | `ENTERO`, `DECIMAL`, `BOOLEANO`, `TEXTO` |
| `descripcion` | text | sí | Qué controla, en lenguaje normal |
| `modificado_por_usuario_id` | bigint | no | |
| `modificado_en` | timestamptz | no | |

**Clave primaria:** `codigo`

Aquí viven el margen de la zona dudosa, los días sin avanzar antes de cerrar una postulación,
el tope de pruebas adicionales y el cupo por defecto de una sesión.

## `plantilla_correo`

Los textos que se envían.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `codigo` | varchar(40) | sí | `POSTULACION_RECIBIDA`, `NO_CONTINUA` |
| `asunto` | text | sí | |
| `cuerpo` | text | sí | Con marcas para los datos que se rellenan |
| `modificada_por_usuario_id` | bigint | no | |
| `modificada_en` | timestamptz | no | |

**Clave primaria:** `codigo`

## `instruccion_ia`

Los textos que se le mandan a la máquina, versionados.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `agente` | varchar(40) | sí | Cuál de los cinco |
| `version` | smallint | sí | |
| `texto` | text | sí | |
| `estado` | varchar(20) | sí | `BORRADOR`, `PUBLICADA`, `RETIRADA` |
| `publicada_por_usuario_id` | bigint | no | Solo Dirección |
| `publicada_en` | timestamptz | no | |

**Clave primaria:** `id` · **Único:** `agente` + `version`

Se pueden cambiar sin volver a desplegar el sistema. Cada calificación guarda con qué versión
de instrucción se produjo.

---

# 13 · Agentes de inteligencia artificial

Hay cinco agentes: el que puntúa el CV, el que califica respuestas abiertas, el que califica
el entregable de la prueba, el que detecta contradicciones y el que redacta las cinco
preguntas de la conversación final.

## `trabajo_ia`

El encargo. Se procesa en segundo plano; el candidato no espera.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `agente` | varchar(40) | sí | `PUNTUAR_CV`, `CALIFICAR_RESPUESTA`, `CALIFICAR_ENTREGABLE`, `DETECTAR_CONTRADICCION`, `GENERAR_PREGUNTAS` |
| `postulacion_id` | bigint | no | Sobre qué postulación |
| `referencia_tabla` | varchar(40) | sí | Sobre qué fila concreta trabaja |
| `referencia_id` | bigint | sí | |
| `estado` | varchar(20) | sí | `PENDIENTE`, `EN_CURSO`, `HECHO`, `FALLIDO` |
| `intentos` | smallint | sí | Cuántas veces se ha probado |
| `creado_en` | timestamptz | sí | |
| `terminado_en` | timestamptz | no | |

**Clave primaria:** `id`

Si el encargo lleva demasiado tiempo en `PENDIENTE` o `EN_CURSO`, se avisa al reclutador. La
postulación **no se mueve y no se inventa una nota**.

## `ejecucion_ia`

Cada intento por separado.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `trabajo_ia_id` | bigint | sí | |
| `agente` | varchar(40) | sí | |
| `modelo` | varchar(60) | sí | Qué modelo se usó |
| `version_modelo` | varchar(40) | no | |
| `instruccion_ia_id` | bigint | no | Con qué versión de instrucción |
| `envio` | text | sí | Lo que se le mandó, entero |
| `respuesta` | text | no | Lo que respondió, **entero** |
| `tokens_entrada` | integer | no | |
| `tokens_salida` | integer | no | |
| `costo` | numeric(10,4) | no | |
| `duracion_ms` | integer | no | |
| `es_exitosa` | boolean | sí | |
| `error` | text | no | |
| `ejecutada_en` | timestamptz | sí | |

**Clave primaria:** `id`

Un encargo reintentado tres veces tiene **tres filas aquí**, y las tres quedan. Si alguien
reclama una calificación hay que poder revisar en qué se basó, incluidos los intentos que
salieron mal.

---

# 14 · Auditoría y seguimiento

## `auditoria`

Toda acción que cambia una decisión.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `usuario_id` | bigint | no | Vacío si fue el sistema |
| `rol_id` | bigint | no | Con qué rol actuaba |
| `accion` | varchar(60) | sí | `AJUSTAR_NOTA`, `CAMBIAR_PERMISO`, `CERRAR_POSTULACION` |
| `entidad` | varchar(40) | sí | Sobre qué tabla |
| `entidad_id` | bigint | no | Sobre qué fila |
| `valor_anterior` | jsonb | no | Qué había antes |
| `valor_nuevo` | jsonb | no | Qué quedó |
| `motivo` | text | no | |
| `ip` | inet | no | |
| `ocurrida_en` | timestamptz | sí | |

**Clave primaria:** `id`

**No se puede modificar ni borrar**, y eso no es configurable: no existe la casilla para
permitirlo. Solo Dirección la consulta.

Es la única tabla donde se usa `jsonb`, porque el valor anterior y el nuevo tienen forma
distinta según qué se cambió. En cualquier otra tabla, un `jsonb` sería pereza.

## `correo_enviado`

A quién, cuándo y **qué decía**.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `usuario_id` | bigint | sí | |
| `plantilla_correo_codigo` | varchar(40) | no | Cuál se usó |
| `asunto` | text | sí | Tal como salió |
| `cuerpo` | text | sí | Tal como salió, ya con los datos rellenados |
| `enviado_en` | timestamptz | sí | |
| `error` | text | no | Si el envío falló |

**Clave primaria:** `id`

Se guarda el cuerpo ya armado, no solo cuál plantilla se usó. Si mañana alguien edita la
plantilla, lo que se le envió a esa persona **sigue siendo lo que dice el registro**.

## `seguimiento_desempeno`

Cómo le fue a los 30, 90 y 180 días de contratado.

| Columna | Tipo | Oblig. | Qué guarda |
|---|---|---|---|
| `id` | bigint | sí | Clave |
| `postulacion_id` | bigint | sí | |
| `dias` | smallint | sí | 30, 90 o 180 |
| `puntaje` | numeric(5,2) | no | |
| `origen` | varchar(30) | sí | De dónde vino el dato |
| `cargado_en` | timestamptz | sí | |

**Clave primaria:** `id` · **Único:** `postulacion_id` + `dias`

Viene de RENASER OS, **un módulo que hoy no existe**. La tabla está prevista y queda vacía
hasta que exista; nada más depende de ella.

Sirve para comparar lo que el sistema predijo con lo que realmente pasó. Esa comparación es la
que dirá, dentro de un par de años, qué preguntas y qué pesos no predicen nada.

---

## Índices

Las claves primarias y los únicos ya crean su índice. Estos son los que hay que añadir a mano.

⚠️ **PostgreSQL no crea índice sobre una clave foránea.** A diferencia de MySQL, hay que
ponerlo uno mismo. Es la causa más común de que una base bien diseñada vaya lenta.

| Índice | Para qué consulta |
|---|---|
| `postulacion (vacante_id, estado_codigo)` | El embudo de una vacante: cuántos hay en cada etapa |
| `postulacion (estado_codigo)` | La bandeja del reclutador: todo lo que espera a una persona |
| `postulacion (usuario_id)` | «Mis postulaciones», en el portal del candidato |
| `postulacion (movido_en)` | Ordenar por días sin avanzar |
| `transicion_estado (postulacion_id, ocurrida_en)` | El historial completo de un candidato |
| `respuesta (evaluacion_id)` | Cargar el examen que rindió |
| `nota_criterio (postulacion_id)` | La ficha del candidato |
| `pregunta (version_banco_id)` | Cargar el banco al empezar una evaluación |
| `trabajo_ia (creado_en) WHERE estado = 'PENDIENTE'` | La cola: qué falta por calificar |
| `ejecucion_ia (trabajo_ia_id)` | Los intentos de un encargo |
| `inscripcion_sesion (sesion_simulacion_id)` | Cuánto cupo queda en una sesión |
| `auditoria (entidad, entidad_id)` | Qué le pasó a esta fila |
| `auditoria (usuario_id, ocurrida_en)` | Qué hizo esta persona |
| `correo_enviado (usuario_id, enviado_en)` | Qué se le ha enviado a alguien |

El de `trabajo_ia` es **parcial**: solo indexa las filas pendientes. Como la cola siempre
pregunta por lo que falta, el índice se mantiene diminuto aunque la tabla tenga millones de
encargos ya hechos.

### Lo que no se arregla con índices

Dos cosas que van a doler si no se cuidan desde el principio, y que no son problema de la base:

**El N+1 del ORM.** Pintar la ficha de un candidato toca una docena de tablas. Si cada
relación se carga sola cuando alguien la mira, Hibernate lanza cientos de consultas en vez de
unas pocas. Hay que decidir explícitamente qué se trae junto.

**La ficha del candidato armada de un tirón.** Junta CV, evaluación, prueba, simulación,
validación, decisión, alertas y auditoría. Va por pestañas: cada una pide lo suyo cuando se
abre. Ninguna optimización salva a una pantalla que pide todo a la vez.

### La tabla que hay que vigilar

`ejecucion_ia` guarda el envío y la respuesta completos del modelo. A unos 7 KB por ejecución
y del orden de 200.000 al año, crece **más rápido que todo el resto junto**: alrededor de
1,5 GB anuales. No es un problema hoy, pero a los dos o tres años conviene mover las
ejecuciones viejas fuera de la base o partir la tabla por fecha.

Todo lo demás es pequeño. Medio millón de filas al año en la tabla más movida —`respuesta`—
no le hace cosquillas a PostgreSQL.

---

## Resumen por área

| Área | Tablas |
|---|---|
| 1 · Personas, acceso y permisos | 7 |
| 2 · Consentimiento y borrado | 3 |
| 3 · Vacantes | 4 |
| 4 · Postulación y su historia | 3 |
| 5 · Criterios y notas | 2 |
| 6 · El CV | 3 |
| 7 · Banco de preguntas | 9 |
| 8 · Evaluación | 6 |
| 9 · Prueba del puesto | 6 |
| 10 · Simulación de 2 horas | 6 |
| 11 · Validación y decisión | 8 |
| 12 · Configuración | 8 |
| 13 · Agentes de inteligencia artificial | 2 |
| 14 · Auditoría y seguimiento | 4 |
| **Total** | **71** |

De esas 71, ocho son catálogos —listas fijas que se cargan una vez— y seis solo unen otras dos
tablas. Las entidades de negocio de verdad son unas cincuenta y siete.

---

## Documentos relacionados

- [Modelo de datos](05-MODELO-DE-DATOS.md) — por qué el modelo es así y qué reglas impone
- [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) — qué hace el sistema
- [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) — tecnología y seguridad
- [Estados de la postulación](03-ESTADOS-POSTULACION.md) — los estados y sus transiciones
- [Roles y permisos](04-ROLES-Y-PERMISOS.md) — quién puede hacer qué
