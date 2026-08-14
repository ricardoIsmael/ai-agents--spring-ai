# Modelo de datos

Cómo se guarda la información del sistema de selección de Renaser: qué tablas existen, cómo
se llaman, por qué existe cada una y qué reglas hace cumplir la base de datos por sí sola.

Este documento se lee solo. No hace falta abrir los otros.

---

## Para qué existe este documento

Es el puente entre lo que el sistema debe hacer y el código que lo hace. Antes de escribir la
primera tabla en Java hay que estar de acuerdo en qué se guarda y cómo se relaciona, porque
cambiar el modelo con datos reales dentro es caro y arriesgado.

Sirve para tres cosas:

- **Escribir las migraciones.** Cada tabla de aquí se convierte en un archivo versionado.
- **Discutir cambios.** Cuando el cliente pida algo nuevo, se mira aquí si cabe o no.
- **Entender el sistema.** Un modelo de datos bien contado explica el negocio mejor que
  cualquier otro documento.

---

## Qué guarda este sistema

Renaser Consulting contrata personal para sí misma. Una persona ve una vacante en el portal
público, crea su cuenta, sube su CV y postula. A partir de ahí atraviesa cinco etapas:

| Etapa | Qué pasa | Quién califica |
|---|---|---|
| 1 · CV | La inteligencia artificial lo puntúa sobre 100 | Máquina |
| 2 · Evaluación integral | Entre 86 y 126 preguntas según el nivel del puesto | Máquina |
| 3 · Prueba del puesto | Trabajo cronometrado, con un cambio inesperado a mitad | Máquina |
| 4 · Simulación de 2 horas | Sesión grupal presencial, cada uno en su pantalla | Persona |
| 5 · Validación de 7 días | Trabajo real dentro de la empresa | Persona |

Al final hay una decisión: **verde** (se contrata), **ámbar** (falta averiguar algo y se pide
una prueba más), **rojo** (no pasa) o **sin datos** (no hay evidencia suficiente).

Los puestos se agrupan en tres niveles —Dirección, Supervisión y Ejecución— y el nivel decide
casi todo: cuántas preguntas responde el candidato, cuánto pesa cada etapa en la nota final y
qué fallos son imperdonables.

Dentro de la empresa trabajan cuatro clases de usuario: el **reclutador**, que lleva el día a
día; el **jefe del área** que contrata, que ve solo a los candidatos de sus vacantes;
**Dirección**, que puede todo y es quien aprueba los cambios de fondo; y el **candidato**, que
solo ve lo suyo.

---

## Cómo se llaman las cosas

Un estándar de nombres aburrido y aplicado sin excepciones. Su valor no es la belleza: es que
cualquiera pueda adivinar el nombre de una columna sin ir a buscarla.

| Regla | Ejemplo |
|---|---|
| Español, pero **sin tildes ni eñes** en los nombres | `contrasena_hash`, `descripcion`, `anonimizado_en` |
| Todo en minúsculas, palabras unidas con guion bajo | `version_banco`, `marca_tiempo_simulacion` |
| Tablas en **singular**: una fila es una cosa | `usuario`, no `usuarios` |
| Clave primaria: siempre se llama `id` | `usuario.id` |
| Clave foránea: el nombre de la tabla a la que apunta, más `_id` | `postulacion.usuario_id` |
| Tabla que une dos: los dos nombres seguidos | `usuario_rol`, `rol_permiso`, `sesion_vacante` |
| Fecha u hora de algo que pasó: termina en `_en` | `creado_en`, `publicado_en`, `entregado_en` |
| Sí o no: empieza por `es_` | `es_final`, `es_sistema`, `es_entrega_automatica` |
| Una nota o puntaje: empieza por `nota_` | `nota_criterio`, `nota_etapa` |
| Una versión de algo: empieza por `version_` | `version_banco`, `version_pesos` |
| Un peso configurable: empieza por `peso_` | `peso_etapa`, `peso_dimension` |

**Sin prefijos de módulo.** La base es solo de selección, así que `sel_vacante` no aportaría
nada y habría que escribirlo setenta y una veces.

**Nada de `_tabla`, `_tbl` ni `_catalogo`** al final de un nombre. Ya se sabe que es una
tabla.

### Los catálogos se identifican con texto, no con números

Las tablas que son listas fijas —los estados de una postulación, las etapas, los niveles de
puesto, las dimensiones— no usan un número como clave, sino un código legible:

```
estado_postulacion.codigo = 'CV_EN_REVISION'
etapa.codigo              = 'PRUEBA_PUESTO'
dimension.codigo          = 'INT'
```

Así, al mirar la tabla de postulaciones se entiende en qué estado está cada una sin cruzarla
con nada. Con un número habría que ir a buscar qué significa el 7 cada vez.

---

## Las siete decisiones que le dan forma

Son los puntos donde este modelo puede salir mal sin que nada falle a la vista. Cada uno está
aquí porque la alternativa parecía más simple y era peor.

### 1 · La persona está separada de la cuenta

Son tres tablas y no una, y conviene entender por qué:

- **`persona`** guarda quién es alguien: nombre, apellidos, teléfono, documento.
- **`usuario`** guarda cómo entra al sistema: correo y contraseña.
- **`postulacion`** es lo que hace un usuario en una vacante concreta.

Un candidato no es una tabla aparte: **es un usuario que postuló**. El equipo de Renaser y la
gente que postula usan la misma puerta, y «Candidato» es un rol como los otros tres.

Esto resuelve tres cosas de golpe. Los datos personales están en **un solo sitio**, así que el
día que alguien pida que se borren no hay que ir a limpiar dos tablas. Alguien del equipo de
Renaser puede postular a otra vacante interna sin tener una ficha duplicada. Y un cambio de
teléfono se hace una vez, no dos.

### 2 · Las respuestas de la evaluación no cuelgan de la postulación

Cuelgan del usuario y de la versión del banco que respondió.

Alguien que respondió noventa preguntas para una vacante de Dirección y luego postula a otra
vacante también de Dirección **no las vuelve a responder**: sus respuestas se reutilizan. Si
cambia de nivel, entonces sí responde el banco que corresponde.

Si las respuestas colgaran de la postulación habría que copiarlas cada vez, y dos copias de la
misma respuesta terminan diciendo cosas distintas.

### 3 · Una versión publicada nunca se modifica

Ni el banco de preguntas, ni los pesos, ni las plantillas de prueba, ni el texto de
consentimiento. Editar cualquiera de esas cosas **crea una versión nueva**; la anterior queda
tal como estaba, para siempre.

Esto es lo que permite que la nota de un candidato no cambie sola. Basta con guardar a qué
versión estaba atado: como esa versión no se mueve, el dato tampoco.

La alternativa —copiar los valores sueltos dentro de cada nota— parece más segura pero es
peor: repite el mismo dato miles de veces y, en cuanto alguien arregla un error de tipeo en
una pregunta, unas copias quedan corregidas y otras no.

### 4 · La clave de puntuación tiene tres formas distintas

No caben en una sola columna:

- **Preguntas de estilo.** No hay respuesta correcta. Cada opción reparte puntos entre
  dimensiones: elegir A suma 2 a *velocidad con criterio* y 1 a *iniciativa*.
- **Preguntas de situación y dilemas.** Cada opción vale un puntaje de 0 a 4: `C=4, D=1, A=1,
  B=0`. A veces solo se define la opción buena y las demás quedan sin puntaje.
- **Preguntas de consistencia y las abiertas.** No tienen clave numérica. Tienen una
  explicación en texto de qué se espera, y las califica la inteligencia artificial con una
  guía de 0 a 4.

Por eso el puntaje por opción **admite estar vacío**, y las dimensiones que suma cada opción
viven en su propia tabla.

### 5 · Los estados de la postulación son un catálogo cerrado

Existe una tabla con los veinticinco estados posibles, pero **no tiene pantalla de
administración**: solo cambia con una migración. Nadie inventa un estado nuevo desde la
interfaz.

Está en una tabla, y no solo en el código, porque cada estado dice de quién se está esperando
algo, y esa es exactamente la consulta que arma la bandeja de trabajo del reclutador:
«muéstrame todo lo que está esperándome a mí».

### 6 · «Solo lo suyo» no es un sí o un no

El jefe del área puede ver candidatos, pero solo los de sus vacantes. El candidato puede ver
postulaciones, pero solo las propias. El reclutador los ve todos.

Es el **mismo permiso con distinto alcance**, así que el alcance es una columna en la relación
entre rol y permiso, con tres valores: propio, sus vacantes, o todo.

Convertirlo en un simple sí o no le abriría al jefe del área los datos de todos los candidatos
de la empresa.

### 7 · Nada se borra, y los agentes de IA guardan cada intento

Cuando alguien ejerce su derecho a que borren sus datos, se vacían los campos que lo
identifican y **se conserva todo lo demás**: el historial de estados, las notas, la auditoría.
Un borrado en cascada destruiría la trazabilidad que el sistema entero existe para tener.

Y para la inteligencia artificial hacen falta dos tablas, no una. Una guarda el **encargo**
—«califica el entregable de esta postulación»— con su estado y cuántas veces se ha intentado.
Otra guarda **cada intento por separado**: qué agente lo hizo, qué se le mandó, qué respondió
entero, con qué modelo, cuánto tardó y cuánto costó. Un encargo reintentado tres veces tiene
tres ejecuciones, y las tres quedan. Con una sola tabla habría que sobrescribir el intento
anterior, que es justo lo que hay que mirar cuando un candidato reclama su nota.

---

## Mapa general

```
   PERSONAS Y ACCESO              CONFIGURACIÓN
  persona · usuario · rol       versiones de pesos
     permiso · area              parámetros · correos
         │                              │
         │  (una persona, una cuenta,   │ (toda nota apunta
         │   varios roles)              │  a una versión)
         ▼                              │
     VACANTES ─────────┐                │
  puesto · nivel        │               │
         │              │               │
         ▼              ▼               ▼
   ┌──────────────────────────────────────────┐
   │            POSTULACIÓN                    │
   │   un estado a la vez · historial completo │
   └──────────────────────────────────────────┘
      │        │         │          │
      ▼        ▼         ▼          ▼
     CV   EVALUACIÓN   PRUEBA   SIMULACIÓN
           ▲             │          │
           │             ▼          ▼
     BANCO DE      VALIDACIÓN DE 7 DÍAS
    PREGUNTAS             │
                          ▼
                      DECISIÓN
                verde · ámbar · rojo · sin datos
                          │
                          ▼
                    SEGUIMIENTO
                 30 · 90 · 180 días


  AGENTES DE IA                  AUDITORÍA
 encargo · ejecución       quién, cuándo, qué cambió, por qué
       │                              ▲
       └── toda nota automática ──────┘
           apunta a su ejecución
```

Dos cosas que el mapa deja ver y conviene subrayar: **la evaluación cuelga del usuario**, no
de la postulación, por lo dicho en la segunda decisión; y **todo lo que califica una máquina
apunta a la ejecución concreta** que produjo esa nota.

Hay una versión dibujada de este mismo mapa en
[diagramas/modelo-de-datos.html](diagramas/modelo-de-datos.html), que se abre en el navegador.

---

## Las tablas

Setenta y una en total, agrupadas por área para poder leerlas de a poco. En cada una se
nombran las columnas que importan para entender qué hace, no todas.

**Para verlas todas, con tipo y clave, está el [Diccionario de datos](07-DICCIONARIO-DE-DATOS.md).**
Ese documento se consulta al escribir las migraciones; este se lee para entender el modelo.

Todas tienen `id` y fecha de creación aunque no se repita cada vez.

---

### Personas, acceso y permisos · 7 tablas

Un rol es un conjunto de permisos con nombre guardado en la base de datos, no algo fijo en el
código. Dirección puede crear roles nuevos y repartir permisos sin que nadie programe.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `persona` | Quién es alguien. Vale para el equipo y para quien postula | nombre, apellidos, telefono, documento, fecha_nacimiento, anonimizado_en |
| `usuario` | Cómo entra al sistema. Apunta a una persona | persona_id, correo, contrasena_hash, area_id, es_activo |
| `area` | El departamento que contrata. Hace falta para saber qué candidatos ve un jefe y para impedir que alguien sea Bar Raiser de su propia área | nombre |
| `rol` | Un nombre y una lista de permisos | codigo, nombre, descripcion |
| `permiso` | Una acción suelta que se puede conceder o no. Son 53 | codigo, etiqueta, grupo |
| `usuario_rol` | Una persona puede tener varios roles. Puede hacer lo que le permita cualquiera de ellos | usuario_id, rol_id |
| `rol_permiso` | Qué permisos tiene un rol y **con qué alcance** | rol_id, permiso_id, alcance |

`persona` y `usuario` están separadas porque son cosas distintas: una es quién eres, la otra
es cómo entras. El `area_id` de un usuario queda vacío en los candidatos, que no pertenecen a
ningún departamento.

El permiso guarda una etiqueta en lenguaje normal —«puede cerrar una vacante»— porque la
pantalla donde Dirección reparte permisos nunca debe mostrar nombres técnicos.

El alcance tiene tres valores: **propio** (solo sus cosas), **sus vacantes** (las del área que
dirige) y **todo**.

---

### Consentimiento y borrado · 3 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `texto_consentimiento` | El texto que se acepta, versionado | version, texto, publicado_en |
| `consentimiento` | Que esta persona aceptó esta versión concreta | persona_id, texto_consentimiento_id, aceptado_en, ip |
| `solicitud_borrado` | Pedir el borrado y ejecutarlo son dos cosas distintas, con días de por medio | persona_id, solicitado_en, ejecutado_en, ejecutado_por_usuario_id |

Se guarda la versión del texto aceptado, no un simple «sí acepté». Si el texto cambia, quienes
ya aceptaron siguen ligados al que firmaron, que es lo que exige la ley 29733.

El texto declara tres cosas: que sus datos se usan para evaluar su postulación, que una
inteligencia artificial participa en la evaluación, y dónde se guardan y por cuánto tiempo.

---

### Vacantes · 4 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `nivel_puesto` | Los tres niveles. Determinan cuántas preguntas se responden y cuánto pesa cada etapa | codigo, nombre, preguntas_banco |
| `puesto` | El catálogo de puestos de Renaser, con su nivel | codigo, nombre, nivel_puesto_codigo |
| `vacante` | Una convocatoria concreta y abierta | puesto_id, titulo, descripcion, requisitos, estado, nota_minima, version_plantilla_prueba_id, jefe_usuario_id, publicada_en, cerrada_en |
| `bar_raiser_asignacion` | Quién revisa como Bar Raiser en esta vacante. Es una función por vacante, no un rol | vacante_id, usuario_id, asignado_por_usuario_id |

El reclutador crea y publica sin que nadie apruebe. Al cerrar una vacante se cierran las
postulaciones que iban a mitad y se avisa a esas personas.

El Bar Raiser **solo opina**: deja su revisión registrada y no puede bloquear una
contratación. El sistema no deja nombrar Bar Raiser al jefe del área que contrata.

---

### Postulación y su historia · 3 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `estado_postulacion` | Catálogo cerrado de los 25 estados | codigo, nombre, espera_a, es_final |
| `postulacion` | Un usuario en una vacante. Tiene un solo estado a la vez, nunca dos | usuario_id, vacante_id, estado_codigo, motivo_cierre, evaluacion_id, pruebas_adicionales_usadas, movido_en |
| `transicion_estado` | Cada cambio de estado, guardado aparte. **No se modifica ni se borra nunca** | postulacion_id, estado_anterior, estado_nuevo, usuario_id, rol_id, es_sistema, motivo, ocurrida_en |

Los estados se agrupan por a quién se está esperando: al candidato, al sistema, al reclutador,
al jefe del área, o a nadie. Los tres finales son «contratado», «no continúa» y «cerrada».

Solo dos estados llevan motivo de cierre, y por eso son dos y no ocho: **no continúa** guarda
si fue por nota baja, fallo grave, decisión roja o decisión de una persona; **cerrada** guarda
si fue por cierre de convocatoria, inactividad, cierre manual, retiro del candidato, ausencia
en la simulación o borrado de datos.

Están separadas porque el candidato recibe mensajes distintos, y porque si se mezclaran, el
embudo de cada vacante mentiría: alguien que se retiró no es alguien que no dio la talla.

El historial de transiciones es lo que permite tres cosas que el sistema promete: reconstruir
el recorrido completo de cualquier candidato, calcular cuánto se tarda en cada etapa, y medir
qué porcentaje de decisiones tomó la máquina sin que interviniera una persona.

Quien hizo el cambio puede ser una persona **o el sistema**, así que `usuario_id` queda vacío
cuando fue automático y `es_sistema` lo dice. Toda transición hecha a mano exige motivo
escrito.

---

### Criterios y notas · 2 tablas

Cuatro etapas puntúan repartiendo 100 puntos entre varios criterios: el CV entre ocho, la
prueba del puesto entre los que defina cada plantilla, la simulación entre diez y la
validación entre nueve métricas. Antes eran ocho tablas —un catálogo y una tabla de notas por
etapa, con las mismas columnas repetidas cuatro veces—. Ahora son dos.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `criterio` | Cualquier cosa que se puntúa, de la etapa que sea | codigo, nombre, etapa_codigo, version_plantilla_prueba_id, puntos |
| `nota_criterio` | El puntaje de un criterio para una postulación, y por qué | postulacion_id, criterio_id, puntaje, explicacion, ejecucion_ia_id, calificada_por_usuario_id, ajustada_por_usuario_id, motivo_ajuste |

Hay dos clases de criterio. **Los globales** —CV, simulación, validación— son iguales para
toda la empresa y su peso vive en la versión de pesos, porque Dirección los cambia y no pueden
cambiar el pasado. **Los de la prueba del puesto** pertenecen a una versión de plantilla y
varían por puesto: la prueba de desarrollador reparte los 100 puntos distinto que la de
ventas. Sus puntos van en la propia fila, porque esa versión de plantilla ya está congelada.

Todas las notas cuelgan de la **postulación**. En la simulación y la validación siempre las
pone una persona, nunca la máquina.

Si mañana hay que añadir «quién revisó esta nota», se toca en un sitio y no en cuatro.

---

### El CV · 3 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `cv` | El currículum de una postulación, en sus dos versiones | postulacion_id, archivo_original_id, archivo_anonimizado_id |
| `enlace_cv` | Portafolio, repositorio, proyectos | cv_id, url, tipo |
| `afirmacion_no_verificada` | Algo que el CV dice sin respaldo | cv_id, texto, estado |

Son **dos archivos, no uno**. Antes de que la máquina lea un CV se le quitan foto, edad, sexo
y estado civil, y esa versión recortada es la única que se le envía. El reclutador sí abre el
original completo. Se guarda cuál de las dos se mandó, para poder demostrar que la regla se
cumplió.

Una afirmación no verificada **no es una mentira**: es algo que hace falta repreguntar. Por
eso tiene estado y no es un simple sí o no.

---

### Banco de preguntas · 9 tablas

Son 236 preguntas: 90 para Dirección, 60 para Supervisión, 50 para Ejecución, y 36 de
alineación personal que responden todos.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `dimension` | Las 22 cosas que se miden: integridad, priorización, calidad, autonomía… | codigo, nombre, definicion |
| `conjunto_dimension` | Algunos pesos se aplican a un par de dimensiones juntas, no a cada una | codigo, nombre |
| `conjunto_dimension_miembro` | Qué dimensiones forman ese conjunto | conjunto_dimension_id, dimension_codigo |
| `version_banco` | Una versión del banco, en borrador o publicada | tipo_banco, nivel_puesto_codigo, etiqueta, estado, publicada_por_usuario_id, publicada_en |
| `pregunta` | Una pregunta dentro de una versión | version_banco_id, codigo, bloque, tipo, enunciado, situacion, logica_interna, disyuntiva, es_puntuable |
| `opcion` | Las opciones de respuesta | pregunta_id, letra, texto, puntaje |
| `opcion_dimension` | Cuánto suma cada opción a cada dimensión | opcion_id, dimension_codigo, incremento |
| `pregunta_dimension` | Qué dimensiones evalúa una pregunta abierta, que no tiene opciones | pregunta_id, dimension_codigo |
| `par_consistencia` | Dos preguntas que miden lo mismo y deberían responderse parecido | version_banco_id, pregunta_a_id, pregunta_b_id, diferencia_maxima |

Los tipos de pregunta son seis: estilo, situación, conductual, microcaso, dilema y
consistencia. Las de **estilo no suman nota**: solo dibujan el perfil de la persona. Las de
**consistencia** tampoco: generan alertas. Eso es lo que dice `es_puntuable`.

El banco lo prepara el reclutador y lo **publica Dirección**. Trabajar en un borrador se puede
hacer cuantas veces se quiera; ponerlo en producción, no.

Una advertencia de nomenclatura que ya causó confusión: las preguntas de autogestión se llaman
C01 a C12, pero el «banco C» es el de Ejecución, cuyas preguntas son O01 a O50. No se puede
deducir a qué banco pertenece una pregunta por la letra de su código. Por eso `version_banco`
lleva su propio `tipo_banco` y no se adivina nada del texto.

---

### Evaluación · 6 tablas

Esta área es la que cuelga del usuario y no de la postulación.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `evaluacion` | Las respuestas de un usuario a un nivel concreto | usuario_id, version_banco_nivel_id, version_banco_alineacion_id, estado, iniciada_en, terminada_en |
| `orden_pregunta` | En qué orden se le mostró cada pregunta y sus opciones. Sin esto no se puede reproducir el examen tal como lo vio | evaluacion_id, pregunta_id, posicion, orden_opciones |
| `respuesta` | Lo que contestó | evaluacion_id, pregunta_id, opcion_id, texto, segundos, respondida_en |
| `nota_respuesta` | El puntaje de esa respuesta y **por qué** | respuesta_id, puntaje, explicacion, ejecucion_ia_id, ajustada_por_usuario_id, motivo_ajuste |
| `resultado_alineacion` | El semáforo de cada uno de los tres bloques | evaluacion_id, bloque, semaforo |
| `alerta` | Contradicciones y respuestas demasiado ideales | postulacion_id, tipo, descripcion, pregunta_a_id, pregunta_b_id, confirmada_por_usuario_id |

Cada respuesta se guarda al momento, así que si se corta la luz el candidato retoma donde
quedó. Una vez enviada no puede volver atrás a cambiarla. No hay plazo para responder.

Las preguntas y las opciones se muestran en orden aleatorio, distinto para cada persona, y ese
orden se guarda: es la única forma de mostrar meses después exactamente el examen que rindió.

Una alerta **nunca descarta a nadie**. Queda visible en la ficha y se convierte en preguntas
para la conversación final. Lo mismo vale para un rojo en alineación personal.

---

### Prueba del puesto · 6 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `plantilla_prueba` | La prueba de un puesto | puesto_id, nombre |
| `version_plantilla_prueba` | Una versión concreta. Si tiene vacante, es una copia privada de esa vacante | plantilla_prueba_id, vacante_id, enunciado, entregables_esperados, duracion_minutos, minuto_cambio, minutos_extra, texto_cambio, estado |
| `intento_prueba` | Cuando un candidato rinde | postulacion_id, version_plantilla_prueba_id, iniciado_en, entregado_en, es_entrega_automatica, cambio_mostrado_en |
| `entregable` | Lo que sube o el enlace que pega | intento_prueba_id, archivo_id, enlace |
| `pregunta_autoevaluacion` | Las 17 preguntas que responde todo el mundo después de entregar | codigo, enunciado |
| `respuesta_autoevaluacion` | Sus respuestas | intento_prueba_id, pregunta_autoevaluacion_id, texto |

El reloj lo lleva el servidor, no el navegador. Si el candidato cierra la página, el tiempo
sigue corriendo. Cuando se acaba, el sistema entrega solo: **no existe entregar tarde**, y por
eso está `es_entrega_automatica`.

El cambio inesperado se dispara solo, en el minuto configurado para esa prueba.

El sistema **no usa detectores de inteligencia artificial**. En vez de eso le pregunta al
candidato qué parte hizo con IA y qué verificó él, y eso vale 5 de los 100 puntos. Se valora a
quien la usa y entiende lo que produjo.

Al reparto de puntos se le avisa si no suma 100, pero **deja guardar igual**: es un aviso, no
un bloqueo. Así que la base no puede exigir que sume 100.

---

### Simulación de 2 horas · 6 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `sesion_simulacion` | Una fecha con cupo. No hay límite de cuántas se crean | fecha_hora, lugar, cupo, estado, creada_por_usuario_id |
| `sesion_vacante` | Para qué vacantes sirve esa sesión: una, varias o todas | sesion_simulacion_id, vacante_id |
| `inscripcion_sesion` | El candidato eligió esta fecha | sesion_simulacion_id, postulacion_id, asistio, marcada_por_usuario_id |
| `momento_simulacion` | Los seis tramos de las dos horas | codigo, nombre, minuto_inicio, minuto_fin |
| `marca_tiempo_simulacion` | Diez momentos que el sistema anota solo: cuándo preguntó, cuándo empezó a trabajar, cuándo reaccionó al cambio… | inscripcion_sesion_id, evento, ocurrida_en |
| `pregunta_generada` | Las cinco preguntas personalizadas para la conversación final | postulacion_id, texto, alerta_id, ejecucion_ia_id |

El candidato solo ve las sesiones de su vacante que tengan cupo. Si no hay ninguna, su
postulación aparece en la bandeja del reclutador como pendiente de programar.

Cuando una sesión se llena deja de ofrecerse; el reclutador puede ampliar el cupo o publicar
otra fecha. Si se cancela, a los inscritos se les avisa y vuelven a quedar pendientes de
elegir.

Las horas se guardan con precisión de segundos y con zona horaria, porque de esas marcas salen
las cinco preguntas de la conversación final.

---

### Validación de 7 días y decisión · 8 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `validacion` | El periodo de trabajo real | postulacion_id, inicio_en, fin_en, estado |
| `etapa` | Las cinco etapas del embudo | codigo, nombre, orden |
| `nota_etapa` | La nota de cada etapa, atada a la versión de pesos con que se calculó | postulacion_id, etapa_codigo, puntaje, version_pesos_id |
| `decision` | El semáforo final | postulacion_id, semaforo, nota_global, version_pesos_id, decidida_por_usuario_id, motivo |
| `tipo_fallo_grave` | Qué no se perdona en cada nivel. Configurable | nivel_puesto_codigo, descripcion, es_activo |
| `fallo_grave` | Uno detectado en un candidato concreto | postulacion_id, tipo_fallo_grave_id, explicacion, ejecucion_ia_id, confirmado_por_usuario_id |
| `opinion_bar_raiser` | Su revisión escrita | postulacion_id, usuario_id, texto |
| `prueba_adicional` | La prueba que se pide cuando sale ámbar | postulacion_id, numero, motivo, enunciado, solicitada_por_usuario_id, puntaje |

La decisión **no es un promedio**. El semáforo tiene cuatro valores, y «sin datos» es distinto
de rojo: significa que falta evidencia, no que la persona falle.

Un fallo grave lo puede detectar la máquina, pero **siempre lo confirma una persona antes de
que bloquee a nadie**. La decisión de contratar es del jefe del área o de Dirección, nunca del
reclutador.

Las pruebas adicionales tienen tope —dos por defecto, configurable—. Al llegar al tope el
sistema ya no permite otra y obliga a decidir verde o rojo con lo que hay.

---

### Configuración · 8 tablas

Casi todo lo que el cliente cambia seguido vive aquí, no en el código.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `version_pesos` | Una versión de todos los pesos, en borrador o publicada | etiqueta, estado, publicada_por_usuario_id, publicada_en |
| `peso_etapa` | Cuánto pesa cada etapa en cada nivel | version_pesos_id, nivel_puesto_codigo, etapa_codigo, peso |
| `peso_componente_evaluacion` | Cómo se reparte la evaluación integral entre psicometría, banco y alineación | version_pesos_id, componente, peso |
| `peso_dimension` | Cuánto pesa cada conjunto de dimensiones en cada nivel | version_pesos_id, nivel_puesto_codigo, conjunto_dimension_id, peso |
| `peso_criterio` | Cuánto vale cada criterio en cada nivel, en las tres etapas globales | version_pesos_id, nivel_puesto_codigo, criterio_id, peso |
| `parametro` | Los valores sueltos: margen de la zona dudosa, días sin avanzar antes de cerrar, tope de pruebas adicionales, cupo por defecto | codigo, valor, tipo, descripcion, modificado_por_usuario_id |
| `plantilla_correo` | Los textos que se envían | codigo, asunto, cuerpo |
| `instruccion_ia` | Los textos que se le mandan a la inteligencia artificial, versionados | agente, version, texto, publicada_por_usuario_id |

La psicométrica todavía no se ha comprado. Su 30% se reparte entre las otras dos partes
mientras tanto, y por eso los pesos de los componentes son datos y no números escritos en el
código: el día que exista, se conecta sin rehacer nada.

Las instrucciones que recibe la máquina son configuración versionada, igual que las preguntas.
Solo Dirección las cambia, y cada calificación guarda con qué versión de instrucción se
produjo.

---

### Agentes de inteligencia artificial · 2 tablas

Hay cinco agentes distintos: el que puntúa el CV, el que califica respuestas abiertas, el que
califica el entregable de la prueba, el que detecta contradicciones y el que redacta las cinco
preguntas de la conversación final.

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `trabajo_ia` | El encargo pendiente. Se procesa en segundo plano; el candidato no espera | agente, postulacion_id, referencia, estado, intentos, creado_en, terminado_en |
| `ejecucion_ia` | Cada intento por separado | trabajo_ia_id, agente, modelo, version_modelo, instruccion_ia_id, envio, respuesta, tokens_entrada, tokens_salida, costo, duracion_ms, es_exitosa |

Se guarda la respuesta **completa**, no solo la nota: si alguien reclama una calificación, hay
que poder revisar en qué se basó.

Tres reglas que la máquina no puede romper:

- **Una nota sin explicación no se acepta.** La explicación es obligatoria.
- **Si falla, la calificación queda pendiente y se reintenta.** Nunca se guarda un cero por un
  problema técnico. Si el encargo lleva demasiado tiempo atascado, se avisa al reclutador.
- **Si falta un dato, la máquina dice que falta.** Nunca lo inventa. Un dato faltante es
  distinto de un cero.

---

### Auditoría y seguimiento · 4 tablas

| Tabla | Para qué existe | Columnas que importan |
|---|---|---|
| `auditoria` | Toda acción que cambia una decisión | usuario_id, rol_id, accion, entidad, entidad_id, valor_anterior, valor_nuevo, motivo, ocurrida_en |
| `archivo` | Los archivos viven fuera de la base; aquí solo está su ruta | ruta, nombre_original, tamano, tipo, subido_en |
| `correo_enviado` | A quién, cuándo y **qué decía** | usuario_id, plantilla_correo_codigo, asunto, cuerpo, enviado_en |
| `seguimiento_desempeno` | Cómo le fue a los 30, 90 y 180 días de contratado | postulacion_id, dias, puntaje, origen |

La auditoría **no se puede modificar ni borrar**, y eso no es configurable: no existe la
casilla para permitirlo. Solo Dirección la consulta. También registra los cambios de permisos:
qué permiso, sobre qué rol, y qué valor tenía antes.

Del correo se guarda el cuerpo ya armado, no solo cuál plantilla se usó. Si mañana alguien
edita la plantilla, lo que se le envió a esa persona sigue siendo lo que dice el registro.

La base guarda la ruta del archivo, nunca el archivo. Así los entregables pesados —videos,
diseños, archivos de hasta 200 MB— no inflan la base de datos. El almacén es privado siempre:
para abrir un archivo, el backend genera un enlace firmado que dura poco.

El seguimiento de desempeño viene de RENASER OS, **un módulo que hoy no existe**. La tabla
está prevista y queda vacía hasta que exista; nada más depende de ella.

---

## Lo que la base impide por sí sola, y lo que no

No todas las reglas del sistema caben en una restricción de base de datos. Conviene tener
claro cuáles sí, porque las que no, hay que probarlas en el código.

### Las hace cumplir la base

- Una postulación tiene **un solo estado a la vez**, y ese estado existe en el catálogo.
- Un usuario no puede postular dos veces a la misma vacante.
- Una persona no puede tener dos cuentas, ni una cuenta pertenecer a dos personas.
- No se puede registrar una respuesta a una pregunta que no está en la versión que le tocó.
- Los registros de auditoría y de transiciones **no se pueden actualizar ni borrar**.
- Toda transición manual y todo ajuste de nota **exigen motivo escrito**.
- El puntaje de una etapa siempre apunta a una versión de pesos concreta.
- No se puede inscribir a un candidato en una sesión que no sirve para su vacante.

### Tienen que vivir en el código

- **Que el reparto de puntos sume 100.** No puede ser restricción porque el sistema avisa pero
  deja guardar igual.
- **Que no se nombre Bar Raiser al jefe del área que contrata.** Requiere mirar la vacante y
  el área en la misma comprobación.
- **Que Dirección no se quede sin permisos.** Son dos reglas distintas: nadie edita su propio
  rol, y no se puede guardar un cambio que dejaría a **cero personas** con permiso para
  administrar roles. La segunda exige contar filas de tres tablas a la vez, así que es una
  comprobación antes de guardar, con un mensaje que explique por qué no se puede.
- **Los límites configurables**, como el tope de dos pruebas adicionales: el número está en la
  tabla de parámetros y puede cambiar, así que la comprobación es del código.
- **Los permisos de cada llamada.** Ocultar un botón no es seguridad. Cada llamada a la API
  comprueba quién es el usuario, qué permiso necesita y con qué alcance.

### Nunca existen, ni siquiera como opción

Cinco cosas que no deben aparecer como permiso, porque si aparecen como casilla alguien las va
a marcar algún día:

1. Que un candidato vea a otros candidatos
2. Que las claves de puntuación lleguen al portal del candidato
3. Que se pueda borrar o modificar la auditoría
4. Que se pueda saltar el consentimiento
5. Que la máquina contrate a alguien sin que intervenga una persona

---

## Cómo se atiende un borrado de datos

Aquí se ve por qué conviene tener `persona` separada de `usuario`: casi todo el borrado ocurre
en una sola tabla.

1. Se registra la solicitud con fecha. Solo Dirección puede ejecutarla.
2. Al ejecutarla se vacían los campos de `persona`: nombre, apellidos, teléfono, documento,
   fecha de nacimiento. Se marca `anonimizado_en`.
3. En `usuario` se anula el correo y se desactiva la cuenta.
4. Se borran sus archivos del almacén —CV y entregables— y las filas quedan apuntando a nada.
5. Sus respuestas de texto libre se vacían, porque pueden contener datos personales.
6. **Se conserva todo lo demás**: puntajes, historial de estados, auditoría, métricas.
7. Sus postulaciones abiertas pasan a cerradas, con motivo «pidió borrar sus datos».

El resultado es que el embudo de esa vacante sigue cuadrando y la auditoría sigue completa,
pero ya no hay forma de saber de quién se trataba.

Retirarse de un proceso **no es lo mismo** que pedir el borrado: quien se retira conserva sus
datos y sus respuestas.

---

## Qué trae la base el primer día

Datos que se cargan con la primera migración, no a mano:

- Los **25 estados** de la postulación
- Los **53 permisos**, con su etiqueta y su grupo
- Los **cuatro roles** iniciales con sus permisos: candidato, reclutador, jefe del área y
  dirección. Es como arranca el sistema, no cómo queda para siempre
- Las **22 dimensiones** y sus conjuntos
- Las **cinco etapas** y los **tres niveles** de puesto
- Los **ocho criterios** del CV y las **nueve métricas** de la validación
- Los **seis momentos** y los **diez criterios** de la simulación
- Las **17 preguntas** de autoevaluación de la prueba
- Las **236 preguntas** del banco, como primera versión publicada
- Las **plantillas de prueba** de los puestos, con sus tiempos
- Una primera **versión de pesos** con los valores acordados
- Los **parámetros** con sus valores por defecto
- Las **plantillas de correo**

---

## Lo que queda pendiente

**Diferencias de conteo con los otros documentos.** Al construir este modelo se contaron las
cosas una por una y salieron números distintos de los que dicen los demás documentos: los
estados de la postulación son **25**, no 24; los permisos son **53**, no «unos 60»; y las
dimensiones son **22**, no 18 —hay cuatro que se usan en preguntas reales pero solo están
definidas en un documento del cliente distinto del principal—. Este modelo usa los números
reales. Los otros documentos están pendientes de corregir.

**El total de preguntas de la evaluación.** Se ha venido diciendo «126 a 138 preguntas». La
suma real es 126 en Dirección, 96 en Supervisión y 86 en Ejecución.

**Los pares de consistencia no están enumerados.** Los documentos del cliente dicen que hay
preguntas que se comparan entre sí para detectar contradicciones, pero nunca dicen cuáles con
cuáles. La tabla está prevista y arranca vacía.

**Si las 36 preguntas de alineación personal dependen del nivel.** Ningún documento lo dice.
El modelo asume que las responden todos por igual.

**El catálogo de puestos.** Los documentos del cliente usan dos juegos de nombres para los
mismos puestos y anuncian doce pruebas pero nombran once. Hay que fijar un catálogo con
nombres definitivos antes de cargar los datos iniciales.

**La figura legal del periodo de 7 días.** Bloquea esa etapa, no el modelo.

**Qué se hace con pgvector.** El proyecto lo trae como dependencia, pero ningún requisito pide
búsqueda por significado todavía. No hay tablas para eso en este modelo.

---

## Documentos relacionados

- [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) — qué hace el sistema
- [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) — tecnología y seguridad
- [Estados de la postulación](03-ESTADOS-POSTULACION.md) — los estados y sus transiciones
- [Roles y permisos](04-ROLES-Y-PERMISOS.md) — quién puede hacer qué
- [Diagrama del modelo](diagramas/modelo-de-datos.html) — se abre en el navegador
