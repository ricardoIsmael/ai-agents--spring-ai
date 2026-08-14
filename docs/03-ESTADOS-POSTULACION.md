# Estados de la postulación

Sistema de selección de personal — Renaser Consulting
Versión 1.1 · 2026-08-14

Este documento define **todos** los estados por los que pasa una postulación y cómo se pasa
de uno a otro. Es la base del backend: ningún estado que no esté aquí puede existir.

## El contexto en cuatro líneas

Un candidato postula en el portal de Renaser y atraviesa cinco etapas: se lee su currículum,
responde una evaluación larga, hace una prueba del puesto cronometrada, asiste a una
simulación de dos horas y trabaja siete días. La inteligencia artificial califica las tres
primeras; las dos últimas siempre las evalúa una persona.

El detalle está en [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md).

---

## La idea que organiza todo

Cada estado responde una sola pregunta: **¿de quién se está esperando algo?**

| Espera a | Qué significa |
|---|---|
| 🔵 **Candidato** | La pelota está en su cancha. El sistema no hace nada |
| ⚙️ **Sistema** | La IA está calificando. Nadie tiene que intervenir |
| 🟡 **Reclutador** | Está frenado esperando a una persona de Renaser |
| 🟢 **Jefe del área** | Está frenado esperando al responsable del puesto |
| ⬛ **Nadie** | La postulación terminó |

Esto no es teoría: la pantalla de inicio del reclutador («Panel de gestión» en los requisitos funcionales) muestra exactamente todo lo
que está en 🟡 y 🟢. Si un estado no dice a quién espera, no sirve.

---

## Los 24 estados

### Entrada

| Estado | Espera a | Qué pasó |
|---|---|---|
| `RECIBIDA` | ⚙️ | Acaba de postular y subir su CV |

### Etapa 1 — CV

| Estado | Espera a | Qué pasó |
|---|---|---|
| `CV_CALIFICANDO` | ⚙️ | La IA está puntuando el CV |
| `CV_EN_REVISION` | 🟡 | Quedó cerca del límite. Una persona decide |

### Etapa 2 — Evaluación Integral

| Estado | Espera a | Qué pasó |
|---|---|---|
| `EVALUACION_PENDIENTE` | 🔵 | Está habilitada. No ha entrado a responder |
| `EVALUACION_EN_CURSO` | 🔵 | Empezó a responder. Puede tardar lo que quiera |
| `EVALUACION_CALIFICANDO` | ⚙️ | Terminó. La IA califica las respuestas abiertas |
| `EVALUACION_EN_REVISION` | 🟡 | Quedó cerca del límite. Una persona decide |

### Etapa 3 — Prueba del puesto

| Estado | Espera a | Qué pasó |
|---|---|---|
| `PRUEBA_PENDIENTE` | 🔵 | Está habilitada. No ha empezado |
| `PRUEBA_EN_CURSO` | 🔵 | **El cronómetro está corriendo** |
| `PRUEBA_CALIFICANDO` | ⚙️ | Entregó. La IA califica el entregable |
| `PRUEBA_EN_REVISION` | 🟡 | Quedó cerca del límite. Una persona decide |

### Etapa 4 — Simulación de 2 horas

| Estado | Espera a | Qué pasó |
|---|---|---|
| `SIMULACION_POR_PROGRAMAR` | 🟡 | Aprobó, pero no hay ninguna sesión con cupo para su vacante |
| `SIMULACION_POR_CONFIRMAR` | 🔵 | Hay sesiones disponibles para su vacante. Debe elegir una |
| `SIMULACION_AGENDADA` | 🔵 | Eligió fecha. Espera el día |
| `SIMULACION_EN_CURSO` | 🔵 | Está en la sesión, trabajando |
| `SIMULACION_CALIFICANDO` | 🟡 | Terminó. Falta calificar y hacer la conversación final |
| `SIMULACION_AUSENTE` | 🟡 | No se presentó. **El reclutador decide**: darle otra fecha (vuelve a `SIMULACION_POR_CONFIRMAR`) o cerrar la postulación |

### Etapa 5 — Validación de 7 días

| Estado | Espera a | Qué pasó |
|---|---|---|
| `VALIDACION_POR_INICIAR` | 🟡 | Aprobó. Falta arreglar el tema legal y la fecha de inicio |
| `VALIDACION_EN_CURSO` | 🟢 | Está trabajando los 7 días |
| `VALIDACION_POR_CALIFICAR` | 🟢 | Terminaron los 7 días. Faltan cargar las 9 métricas |

### Decisión

| Estado | Espera a | Qué pasó |
|---|---|---|
| `DECISION_PENDIENTE` | 🟡 | Todo evaluado. Falta la decisión final |
| `PRUEBA_ADICIONAL` | 🔵 | Salió **ámbar**. Se le pidió una prueba específica |

### Finales

| Estado | Espera a | Qué pasó |
|---|---|---|
| `CONTRATADO` | ⬛ | Se le contrató |
| `NO_CONTINUA` | ⬛ | No pasó la evaluación |
| `CERRADA` | ⬛ | Terminó sin llegar a una decisión de fondo |

---

## Por qué solo dos estados finales de cierre

`NO_CONTINUA` y `CERRADA` parecen iguales pero no lo son, y hay que separarlos por dos razones:

**El candidato recibe mensajes distintos.** A quien no pasó se le agradece la participación
(ver «Avisos»). A quien se cerró porque la vacante cerró se le explica eso (ver «Vacantes»). Decirle a alguien
que "no pasó la evaluación" cuando en realidad la vacante se canceló es un error feo.

**Las métricas necesitan distinguirlos.** El embudo por vacante (ver «Panel de gestión») mide dónde se cae la
gente. Si mezclas al descartado con el que quedó a mitad porque cerró la convocatoria, el
embudo miente.

Cada uno guarda además su **motivo**:

| Estado | Motivos posibles |
|---|---|
| `NO_CONTINUA` | Nota debajo del mínimo · Fallo grave confirmado · Decisión roja · Decisión de una persona |
| `CERRADA` | Se cerró la convocatoria · Sin avanzar 60 días (configurable) · Cierre manual · **El candidato se retiró** · No fue a la simulación · Pidió borrar sus datos |

Así son 2 estados con motivo, en vez de 8 estados. Más simple de programar y más fácil de
consultar.

---

## El recorrido normal

```
   RECIBIDA
      |
      v
   CV_CALIFICANDO ---- cerca del limite ----> CV_EN_REVISION
      |                                            |
      | aprueba                          aprueba <-+-> no aprueba
      v                                    |             |
   EVALUACION_PENDIENTE  <-----------------+             v
      |                                             NO_CONTINUA
      v
   EVALUACION_EN_CURSO
      |
      v
   EVALUACION_CALIFICANDO -- cerca del limite --> EVALUACION_EN_REVISION
      |                                                  |
      v                                                  |
   PRUEBA_PENDIENTE  <----------------------------------+
      |
      v
   PRUEBA_EN_CURSO          <-- cronometro corriendo
      |
      v
   PRUEBA_CALIFICANDO ------ cerca del limite ---> PRUEBA_EN_REVISION
      |                                                  |
      v                                                  |
   SIMULACION_POR_PROGRAMAR  <-------------------------+
      |
      v
   SIMULACION_POR_CONFIRMAR
      |
      v
   SIMULACION_AGENDADA -------- no fue -------> SIMULACION_AUSENTE
      |
      v
   SIMULACION_EN_CURSO
      |
      v
   SIMULACION_CALIFICANDO
      |
      v
   VALIDACION_POR_INICIAR
      |
      v
   VALIDACION_EN_CURSO
      |
      v
   VALIDACION_POR_CALIFICAR
      |
      v
   DECISION_PENDIENTE
      |
      +---- verde ----> CONTRATADO
      |
      +---- ambar ----> PRUEBA_ADICIONAL --> vuelve a DECISION_PENDIENTE
      |
      +---- rojo -----> NO_CONTINUA
```

Desde **cualquier** estado que no sea final se puede llegar a `CERRADA`.

---

## Las tres reglas que gobiernan las transiciones

### Regla 1 · La IA nunca descarta en la zona dudosa

Cada etapa que califica la IA tiene tres salidas, no dos:

```
         nota
          |
   +------+------+
   |      |      |
 debajo  zona  arriba
 del     dudosa  del
 minimo    |    minimo
   |       |      |
   v       v      v
NO_      EN_    sigue
CONTINUA REVISION
         (🟡)
```

La zona dudosa es un rango configurable alrededor de la nota mínima. Ejemplo: si el mínimo es
70 y el margen es 5, todo lo que caiga entre 65 y 75 espera revisión humana.

**Además**, una postulación entra en revisión aunque la nota sea clara si:
- La IA detectó un fallo grave (ver «La decisión») — siempre lo confirma una persona.
- Hay una contradicción o una alerta de respuestas ideales (ver «Alertas») — no descarta, pero se ve.

### Regla 2 · Una persona puede mover una postulación a donde quiera

Los requisitos funcionales lo dicen en «La decisión»: cualquier persona con permiso puede cambiar una decisión del sistema, en
cualquier dirección. Eso significa que **todas** las transiciones existen manualmente, incluso
las que el sistema nunca haría solo: devolver a alguien ya descartado, saltarse una etapa,
reabrir una postulación cerrada.

Toda transición manual guarda quién, cuándo, de qué estado a cuál y por qué (ver «Auditoría» en los no funcionales).
El motivo es **obligatorio**.

### Regla 3 · Los estados en ⚙️ tienen que avanzar solos

`CV_CALIFICANDO`, `EVALUACION_CALIFICANDO` y `PRUEBA_CALIFICANDO` dependen de que la IA
responda. Si la IA falla, la postulación **no se mueve y no se inventa una nota** (ver «Inteligencia artificial» en los no funcionales).

Por eso:
- Se reintenta automáticamente.
- Si lleva demasiado tiempo ahí, se avisa al reclutador.
- Nunca se guarda un cero por un fallo técnico.

⚠️ Sin esta regla, un problema de la IA se convierte en un candidato descartado en silencio.

---

## Casos especiales

### El cronómetro se agota

Si el tiempo de `PRUEBA_EN_CURSO` termina, el sistema **entrega solo** lo que haya y pasa a
`PRUEBA_CALIFICANDO`, marcando que fue por tiempo agotado.

No hay estado de "no terminó a tiempo": entregar tarde no existe, se entrega lo que hay.
El tiempo lo controla el servidor, así que cerrar el navegador no lo detiene (ver «Cronómetros» en los no funcionales).

### El candidato ya respondió las preguntas antes

Si postula a otra vacante del mismo nivel (ver «Portal del candidato»), la postulación **se salta** los cuatro
estados de evaluación y entra directo a `PRUEBA_PENDIENTE`. Su nota se copia con la versión
del banco con que la obtuvo.

Si el nuevo puesto es de otro nivel, responde el banco que corresponde con normalidad.

### La postulación lleva mucho tiempo sin moverse

Estar en 🔵 no tiene plazo: el candidato responde cuando quiera (ver «Reglas del examen»). Pero el panel muestra
cuántos días lleva sin avanzar (ver «Panel de gestión»), y hay cuatro formas de cerrarla:

1. Automática, tras **60 días sin actividad** (configurable).
2. Manual, por el reclutador o el administrador.
3. Automática, al cerrar la convocatoria.
4. **El candidato la retira** desde su portal.

En los cuatro casos termina en `CERRADA` con su motivo.

### El candidato se retira

Desde su portal puede retirar cualquier postulación en curso, con un botón. Pasa a `CERRADA`
con ese motivo y deja de recibir avisos de esa vacante.

Retirarse **no borra sus datos**: sus respuestas y notas se conservan. Para borrarlos hay que
pedirlo aparte (ver «Datos personales» en los no funcionales), que es otra cosa.

### Salió ámbar

**Qué es el ámbar.** Es el color del medio del semáforo de la decisión final (ver «La decisión»):

| | Qué significa |
|---|---|
| 🟢 Verde | Todo cuadra. Se contrata |
| 🟡 Ámbar | La persona vale, **pero hay algo que no cuadra** y hay que averiguarlo |
| 🔴 Rojo | Falla algo indispensable. No se contrata |
| ⬜ Sin datos | Falta evidencia. Se pide otra prueba, no se asume que falla |

**Un ejemplo.** Ana saca 86 de 100. En las preguntas dijo que avisa los riesgos temprano.
Pero en la simulación detectó un bloqueo a las 10:41 y lo comunicó a las 10:49: ocho minutos
callada. Su nota es buena, pero **lo que dijo y lo que hizo no coinciden**.

No se la puede descartar por eso, ni contratar sin saber qué pasó. Sale ámbar.

**Qué hace el sistema.** No descarta ni contrata. Crea una prueba corta dirigida a esa duda
concreta y la postulación pasa a `PRUEBA_ADICIONAL`. Cuando el candidato la entrega, vuelve a
`DECISION_PENDIENTE` con la evidencia nueva: si responde bien pasa a verde, si confirma el
problema pasa a rojo.

**Cuántas veces puede repetirse.** Puede salir ámbar otra vez, y otra. Por eso hay un tope:
**máximo 2 pruebas adicionales**, configurable. Al llegar al tope, el sistema ya no permite
otra prueba y obliga a decidir verde o rojo con la evidencia que hay.

Sin ese tope, una postulación puede quedar dando vueltas para siempre.

### El candidato pide borrar sus datos

Los requisitos no funcionales le dan ese derecho en «Datos personales». La postulación pasa a `CERRADA` con ese motivo, se borran sus datos
personales y sus respuestas, pero **el registro de auditoría se conserva** sin datos que
identifiquen a la persona.

Así se cumple la ley sin destruir la trazabilidad de las decisiones que ya se tomaron.

---

## Decisiones tomadas sobre estos casos

| Qué | Decisión |
|---|---|
| Si alguien no va a la simulación | **Lo decide el reclutador.** El sistema le ofrece las dos opciones: darle otra fecha o cerrar la postulación |
| Cuánto tiempo sin avanzar antes de cerrar sola | **Configurable**, con 60 días por defecto |
| Si el candidato puede retirarse solo | **Sí.** Tiene un botón en su portal para retirar su postulación |
| Cuántas veces puede repetirse el ámbar | **Máximo 2** pruebas adicionales, configurable. Al llegar al tope, se decide verde o rojo |

---

## Cómo se guarda esto

**Una postulación tiene un solo estado a la vez.** Nunca dos.

Cada cambio de estado se guarda como un registro aparte que nunca se modifica ni se borra
(ver «Auditoría» en los no funcionales), con: postulación, estado anterior, estado nuevo, quién lo hizo (una persona o el
sistema), cuándo, y por qué.

Eso permite tres cosas que los requisitos piden:
- Reconstruir el recorrido completo de cualquier candidato.
- Calcular cuánto tardó cada etapa, para las métricas de Dirección (ver «Panel de gestión»).
- Saber qué porcentaje de decisiones tomó la IA sin intervención humana (ver «Panel de gestión»).

---

# Documentos relacionados

| Documento | Qué contiene |
|---|---|
| [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) | Qué hace el sistema, etapa por etapa |
| [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) | Tecnología, seguridad, rendimiento |
| [Roles y permisos](04-ROLES-Y-PERMISOS.md) | Quién puede hacer qué, acción por acción |
| [Estados, en un dibujo](diagramas/estados-postulacion.html) | El ciclo que se repite en cada etapa |
