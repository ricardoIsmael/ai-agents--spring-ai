# Requisitos funcionales

Sistema de selección de personal — Renaser Consulting
Versión 1.1 · 2026-08-14

> Este documento se lee solo. No hace falta abrir ningún otro para entenderlo.
> Los detalles largos están en documentos aparte y se enlazan donde corresponde.

---

## Para qué existe este sistema

Renaser quiere contratar **por evidencia de lo que la persona puede hacer**, no por lo que
dice su currículum. Hoy esa evidencia se junta a mano y tarde: se descubre cómo trabaja
alguien recién cuando ya está contratado.

El sistema adelanta esa evidencia y la ordena. Antes de decidir, Renaser ya vio a la persona
resolver un problema real del puesto, adaptarse a un cambio inesperado y trabajar siete días.

La pregunta que el sistema ayuda a responder es siempre la misma:

> *¿Puedo confiarle a esta persona un resultado, un equipo o una parte de Renaser y
> encontrarla mejor de como se la entregué, sin tener que perseguirla ni descubrir tarde sus
> problemas?*

Tres ideas guían todo el diseño:

**No se selecciona por antigüedad.** Diez años repitiendo el mismo trabajo pesa menos que dos
años con resultados demostrables. El currículum abre la puerta, no decide.

**El trabajo real manda sobre el examen.** Si alguien saca 95 en la evaluación pero no produce
en la prueba ni en los siete días, gana lo que hizo, no lo que respondió.

**La máquina prepara, la persona decide lo caro.** La inteligencia artificial hace el trabajo
repetitivo —leer, puntuar, ordenar, avisar— para que el tiempo de las personas se gaste donde
de verdad hace falta: mirar a los candidatos que llegaron lejos.

---

## Qué es este sistema

Un sistema que acompaña a un candidato desde que ve una vacante hasta que es contratado,
y que después comprueba si la persona rindió como el sistema predijo.

Tiene **dos caras**:

| Cara | Quién la usa | Dónde vive |
|---|---|---|
| Portal de empleo | Candidatos | Página pública propia de Renaser |
| Panel de gestión | Equipo de Renaser | Dentro de RENASER OS |

Renaser publica **solo sus propias vacantes**. No es una bolsa de trabajo para terceros.

---

## Alcance

### Qué entra

| | |
|---|---|
| Publicar vacantes | Crear, editar, publicar y cerrar |
| Portal público | Ver vacantes, crear cuenta, postular, subir currículum |
| Evaluación completa | Las cinco etapas, de la lectura del currículum a los siete días |
| Calificación con IA | Currículum, preguntas abiertas y entregables |
| Agenda de la simulación | Fechas, cupos, confirmación y asistencia |
| Decisión | Semáforo, fallos graves, ajustes de una persona |
| Seguimiento | Cómo rindió a los 30, 90 y 180 días |
| Panel y métricas | Embudo, tiempos, aciertos de la predicción |
| Administración | Preguntas, pruebas, pesos, roles, textos de correo |

### Qué no entra

| | Por qué |
|---|---|
| Buscar candidatos en Indeed o CompuTrabajo | Renaser quiere su propio portal |
| Detectar si el candidato usó inteligencia artificial | Usarla no está prohibido; se evalúa si entiende lo que produjo |
| Nómina, contratos, vacaciones | Es otro sistema |
| La prueba psicométrica | Se compra a un tercero; el sistema deja el hueco listo |
| Evaluación de desempeño continua | Solo se registran los cortes de 30, 90 y 180 días |

### Lo que el sistema nunca hace

- No puntúa edad, sexo, embarazo, raza, religión, discapacidad ni orientación sexual.
- No usa la familia, la pareja ni la salud para predecir el desempeño.
- No diagnostica nada psicológico.
- No descarta a nadie por las preguntas de alineación personal.
- No inventa una nota cuando la inteligencia artificial falla: la deja pendiente.

---

## El recorrido completo

```
  CANDIDATO                         SISTEMA

  ve la vacante
  crea su cuenta
  postula y sube CV      ------>    la IA puntúa el CV
                                    descarta a los que no dan el perfil
        |
        v
  responde la
  Evaluación Integral    ------>    corrige solo lo cerrado
  (126 a 138 preguntas)             la IA califica lo abierto
        |
        v
  hace la prueba
  del puesto             ------>    cronómetro corriendo
  (duración editable)               cambio inesperado a mitad
                                    la IA califica el entregable
        |
        v                           descarta a los que no aprueban
  asiste a la
  Simulación de 2 h      ------>    registra tiempos de todo
  (grupal, en sala)                 conversación humana al final
        |
        v
  trabaja 7 días         ------>    el jefe carga 9 métricas
        |
        v
                                    DECISIÓN: verde / ámbar / rojo
        |
        v
  contratado             ------>    seguimiento a 30, 90 y 180 días
                                    ¿acertó la predicción?
```

**Los pesos de la nota final** cambian según el nivel del puesto:

| Etapa | Dirección | Supervisión | Ejecución |
|---|---|---|---|
| CV | 5% | 5% | 5% |
| Evaluación Integral | 20% | 20% | 15% |
| Prueba del puesto | 30% | 30% | 40% |
| Simulación de 2 h | 25% | 25% | 20% |
| Validación de 7 días | 20% | 20% | 20% |

Cuanto más operativo es el puesto, más pesa demostrar el trabajo. Cuanto más alto, más pesan
el criterio y la capacidad de dirigir.

### Las mismas etapas, con otros nombres

Renaser cambió el vocabulario entre un documento y otro. Son las mismas etapas: solo se
tradujeron los anglicismos. En conversación el cliente sigue usando las palabras viejas.

| Como se dice aquí | Como lo llama el cliente | Qué es |
|---|---|---|
| Prueba del puesto | Challenge · Work sample | Etapa 3 |
| **Simulación de 2 horas** | **Kickoff** | Etapa 4 |
| **Validación de 7 días** | **Trial** | Etapa 5 |

⚠️ **La simulación dura 2 horas, no 7 días.** Es fácil confundirlas porque el cliente las
menciona juntas. Van en este orden: primero la sesión de 2 horas en un solo día, después los
7 días de trabajo real.

---

## Dónde trabaja una persona y dónde trabaja la máquina

Esta es la parte que más se pregunta. Hay tres categorías, y conviene no mezclarlas.

### 1 · La máquina sola, siempre

Nadie interviene en esto:

- Puntuar el currículum y descartar a quien está claramente por debajo
- Corregir las preguntas de opción múltiple contra la clave
- Calificar las respuestas escritas con la guía de 0 a 4
- Calificar el entregable de la prueba del puesto
- Detectar contradicciones y respuestas demasiado ideales
- Controlar los cronómetros y soltar el cambio inesperado
- Registrar los tiempos durante la simulación
- Enviar todos los correos
- Cerrar postulaciones abandonadas o por cierre de convocatoria

### 2 · Una persona, siempre

Aquí la inteligencia artificial no decide nunca:

| Qué | Quién |
|---|---|
| Calificar la simulación de 2 horas | Reclutador o jefe del área |
| La conversación final de 15–20 minutos | Reclutador o jefe del área |
| Cargar las 9 métricas de los siete días | Jefe del área o Recursos Humanos |
| **Decidir la contratación** | Jefe del área o Dirección |
| Publicar una versión del banco de preguntas | Dirección |

### 3 · Una persona, solo si hace falta

Estas no pasan siempre. Son excepciones, y son las que aparecen en la pantalla de inicio del
reclutador:

| Cuándo | Qué hace |
|---|---|
| Alguien queda **cerca de la nota mínima** | Decide si avanza |
| La IA detecta un **fallo grave** | Lo confirma o lo descarta |
| No está de acuerdo con una nota | La ajusta, escribiendo por qué |
| Alguien **falta a la simulación** | Le da otra fecha o cierra su postulación |
| La decisión sale **ámbar** | Pide una prueba adicional |

### La forma que tiene esto

La automatización es alta al principio y baja a cero al final. Es a propósito: al inicio hay
mucha gente y equivocarse cuesta poco; al final quedan pocos y la decisión es cara.

Con 100 postulantes, en números aproximados:

```
100 postulan
      |   la IA descarta ~60 sola
      |   ~10 quedan en el límite ---> una persona revisa 10
 40 siguen
      |   la IA descarta ~15 sola
      |   ~5 en el límite         ---> una persona revisa 5
 25 siguen
      |   la IA descarta ~10 sola
      |   ~4 en el límite         ---> una persona revisa 4
 12 llegan a la simulación        ---> una persona califica 12
                                       y conversa con 12
  3 llegan a los siete días       ---> el jefe evalúa 3
  3 decisiones                    ---> el jefe o Dirección deciden 3
```

Los números son un ejemplo, no una promesa. Pero la forma es la real: **de 100 postulantes,
una persona toca a unos 19** en vez de a los 100.

Esto cumple las metas de automatización que fijó Renaser: 90–95% en la primera etapa,
95–100% en la segunda, 80–95% en la tercera y 70–90% en la simulación.

### La palanca que controla todo esto

**La zona dudosa es configurable**, y es lo que decide cuánto trabajo humano hay.

Si el margen es de 5 puntos alrededor de la nota mínima, muy poca gente cae en revisión. Si
se sube a 15, cae mucha más. Renaser puede empezar con un margen amplio, ver qué tan seguido
acierta la inteligencia artificial, y cerrarlo cuando gane confianza.

---

# 1. Vacantes

**RF-01** El reclutador crea una vacante y la publica directo, sin que nadie la apruebe.

**RF-02** Cada vacante define: puesto, nivel (Dirección, Supervisión o Ejecución),
descripción, requisitos, qué prueba se aplica y qué nota mínima se necesita para avanzar.

**RF-03** El nivel de la vacante determina qué preguntas responde el candidato:
90 para Dirección, 60 para Supervisión, 50 para Ejecución.

**RF-04** Al crear la vacante, el reclutador elige una plantilla de prueba ya cargada y
puede modificarla para esa vacante sin alterar la plantilla original.

**RF-05** Una vacante puede cerrarse en cualquier momento. Al cerrarla, las postulaciones
que estén a mitad se cierran también y se avisa a esas personas.

**RF-06** El sistema permite varias vacantes abiertas a la vez.

---

# 2. Portal del candidato

**RF-07** Cualquier persona puede ver las vacantes abiertas sin registrarse.

**RF-08** Para postular hay que crear una cuenta con correo y contraseña.

**RF-09** Al crear la cuenta, el candidato acepta el tratamiento de sus datos y que una
inteligencia artificial participe en su evaluación. El sistema guarda fecha, hora, versión
del texto aceptado y dirección desde donde se aceptó.

**RF-10** El candidato ve en todo momento en qué etapa está cada una de sus postulaciones y
qué le toca hacer.

**RF-11** Todas las evaluaciones se responden **dentro del portal**. El correo solo avisa que
hay algo pendiente; nunca contiene las preguntas.

**RF-12** El candidato puede postular a varias vacantes.

**RF-13** Si ya respondió las preguntas de un nivel y postula a otro puesto del mismo nivel,
sus respuestas se reutilizan y no las repite. Si el nuevo puesto es de otro nivel, responde
el banco que corresponde.

---

# 3. Etapa 1 — El CV

**RF-14** El candidato sube su CV al postular y puede agregar enlaces (portafolio, GitHub,
proyectos).

**RF-15** Antes de que la IA lea el CV, el sistema **oculta foto, edad, sexo y estado civil**.
La IA solo ve formación, experiencia y logros.

**RF-16** El reclutador sí ve el CV completo cuando lo abre.

**RF-17** La IA puntúa el CV sobre 100 con ocho criterios, cuyo peso cambia según el nivel:

| Criterio | Dirección | Supervisión | Ejecución |
|---|---|---|---|
| Resultados demostrables | 25 | 20 | 20 |
| Complejidad y alcance | 15 | 10 | 5 |
| Sistemas o procesos creados | 15 | 15 | 10 |
| Desarrollo de personas | 15 | 15 | 0 |
| Velocidad de aprendizaje | 10 | 10 | 15 |
| Iniciativa | 5 | 10 | 10 |
| Habilidades del puesto | 5 | 10 | 25 |
| Calidad de la evidencia | 10 | 10 | 15 |

**RF-18** La antigüedad no da puntos por sí sola. Diez años repitiendo el mismo trabajo vale
menos que dos años con resultados demostrables.

**RF-19** Cuando el CV afirma algo sin respaldo, el sistema lo marca como **no verificado**.
No lo trata como mentira.

**RF-20** La IA descarta sola a quien queda claramente por debajo de la nota mínima. Los que
quedan cerca del límite esperan a que una persona revise.

**RF-21** Cada puntaje guarda su explicación: qué criterio sumó cuántos puntos y por qué.

---

# 4. Etapa 2 — Evaluación Integral

Para el candidato es **una sola evaluación**. Por dentro son tres partes.

| Parte | Peso | Estado |
|---|---|---|
| Prueba psicométrica | 30% | No existe todavía |
| Banco de preguntas por nivel | 50% | 90, 60 o 50 preguntas |
| Alineación personal | 20% | 36 preguntas |

**RF-22** Mientras Renaser no compre la prueba psicométrica, su 30% se reparte entre las
otras dos: el banco pasa a 71,4% y la alineación personal a 28,6%. Cuando la prueba exista,
se conecta sin rehacer nada.

## Tipos de pregunta

**RF-23** El banco tiene cinco tipos y cada uno se corrige distinto:

| Tipo | Cómo se corrige | ¿Suma nota? |
|---|---|---|
| Elección de estilo | No tiene respuesta correcta | **No.** Solo dibuja el perfil |
| Situación con opciones | Contra la clave, de 0 a 4 | Sí |
| Contar un caso real | La IA con guía de 0 a 4 | Sí |
| Microcaso | La IA con guía de 0 a 4 | Sí |
| Control de consistencia | Compara con otras respuestas | Genera alertas |

**RF-24** Las preguntas de estilo (las primeras 15 de Dirección, 10 de Supervisión y 10 de
Ejecución) no suman nota. Producen un gráfico del estilo de la persona, útil para la
entrevista final.

**RF-25** Las preguntas de texto libre las califica la IA de 0 a 4 con esta guía:

| Puntos | Cuándo |
|---|---|
| 0 | No da un caso, responde en abstracto o evade |
| 1 | Cuenta un caso pero fue pasivo, sin acción propia |
| 2 | Hubo acción clara, pero sin medición ni aprendizaje |
| 3 | Actuó por iniciativa, con criterio y resultado verificable |
| 4 | Anticipó, priorizó, comunicó, midió y convirtió el aprendizaje en un sistema |

**RF-26** Cada nota que pone la IA guarda su explicación.

## Alineación personal

**RF-27** Son 36 preguntas repartidas en tres bloques:

| Bloque | Cuántas | Peso | Sobre qué |
|---|---|---|---|
| Relación con el dinero y el trabajo | 12 | 35% | Incentivos, largo plazo, integridad |
| Madurez en el conflicto | 12 | 35% | Límites, reparación, aceptar correcciones |
| Autogestión | 12 | 30% | Sostener calidad, avisar antes de fallar |

**RF-28** El resultado es un semáforo verde, ámbar o rojo por bloque.

**RF-29** **Un rojo no descarta a nadie.** Genera preguntas concretas para la conversación
final de la simulación, donde una persona lo resuelve.

## Reglas del examen

**RF-30** Las preguntas y sus opciones se muestran en orden aleatorio, distinto para cada
candidato. El sistema guarda ese orden para poder reproducir el examen exacto.

**RF-31** Cada respuesta se guarda al momento. Si se corta la conexión, el candidato retoma
donde quedó.

**RF-32** No puede volver atrás a cambiar lo ya respondido.

**RF-33** El sistema registra cuánto tardó en cada pregunta.

**RF-34** El candidato **nunca** ve las claves, los nombres internos de las dimensiones, el
puntaje de cada opción ni la lógica interna.

**RF-35** No hay plazo para empezar ni para terminar esta etapa.

## Alertas

**RF-36** El sistema detecta cuando un candidato se contradice entre dos preguntas que miden
lo mismo desde ángulos distintos.

**RF-37** El sistema detecta cuando alguien siempre elige la respuesta más ideal sin
reconocer ningún límite ni contrapeso.

**RF-38** Las dos alertas **no descartan a nadie**. Quedan visibles en la ficha y se
convierten en preguntas para la conversación final.

---

# 5. Etapa 3 — Prueba del puesto

**RF-39** Cada prueba tiene cuatro momentos: comprende el problema, produce el entregable,
explica lo que hizo, y se adapta a un cambio inesperado.

**RF-40** El candidato empieza cuando quiere, pero **al empezar el cronómetro corre** y no se
detiene.

**RF-41** Cada prueba tiene **tres tiempos, y los tres se editan desde el sistema**:

| Tiempo | Qué es |
|---|---|
| Duración | Cuánto dura la prueba completa |
| Minuto del cambio | En qué momento aparece el cambio inesperado |
| Tiempo extra | Cuánto tiene para adaptarse tras el cambio |

Ejemplo de cambio inesperado: *"ahora cada horario admite máximo 5 personas, impleméntalo"*.

**RF-42** El sistema arranca con estos valores, tomados del documento de Renaser. Ninguno es
definitivo: todos se cambian desde la pantalla de pruebas.

| Puesto | Duración | Cambio en el minuto | Extra |
|---|---|---|---|
| Director | 120 | 84 | 15 |
| Desarrollador web | 120 | 84 | 20 |
| Talento / RR.HH. | 90 | 63 | 15 |
| Crecimiento / Marketing | 90 | 63 | 15 |
| Diseñador gráfico | 90 | 63 | 15 |
| Producto / UX | 90 | 63 | 15 |
| Editor de video | 75 | 52 | 15 |
| Ventas | 60 | 42 | 15 |
| Seguimiento de clientes | 60 | 42 | 15 |
| Coordinador de Operaciones | 90 ⚠️ | 63 | 15 |
| Compra de medios | 60 ⚠️ | 42 | 15 |

⚠️ **El documento no dice cuánto duran estas dos.** Los valores son una propuesta nuestra,
por parecido con puestos similares. Renaser debe confirmarlos o cambiarlos.

Tampoco dice **en qué minuto** aparece el cambio en ninguna prueba, solo cuánto dura después.
Los minutos de la columna del medio salen de aplicar el **70% de la duración**, que es la
proporción que usa la simulación de 2 horas (el cambio entra en el minuto 85 de 120).

**RF-43** Cuando alguien crea una prueba nueva, el sistema propone el minuto del cambio al
70% de la duración. Es una sugerencia: se puede sobrescribir.

**RF-44** Hay pruebas cuyo enunciado simula un horario propio. La del Coordinador de
Operaciones dice *"son las 9:00, organiza hasta las 18:00"*: eso es la historia del caso, no
el tiempo real del candidato.

La pantalla debe **separar visualmente las dos cosas**, para que nadie confunda la hora del
caso con el tiempo que le queda:

```
  TIEMPO RESTANTE  01:12:44        <- reloj real, arriba y destacado
  ----------------------------------
  En el caso son las 9:00 y debes    <- parte del enunciado
  organizar la jornada hasta las 18:00
```

**RF-45** El candidato entrega subiendo archivos o pegando enlaces (Figma, GitHub, Drive).
El mismo mecanismo sirve para diseño, código, video o documentos.

**RF-46** Al terminar, el candidato responde 17 preguntas sobre su propio trabajo: qué
problema entendió, qué supuestos hizo, qué parte hizo con inteligencia artificial y qué
decidió él, qué verificó antes de entregar, dónde puede fallar su solución.

**RF-47** El sistema **no usa detectores de inteligencia artificial**. Usar IA no está
prohibido: se evalúa si la persona entiende y verifica lo que produjo. Eso vale 5 puntos.

**RF-48** La IA califica el entregable sobre 100:

| Qué se mide | Puntos |
|---|---|
| Resultado producido | 25 |
| Calidad | 15 |
| Comprensión del problema | 10 |
| Velocidad y manejo del tiempo | 10 |
| Criterio en las decisiones | 10 |
| Capacidad de explicar lo hecho | 10 |
| Uso inteligente de la IA | 5 |
| Orientación a resultados medibles | 5 |
| Adaptación al cambio | 5 |
| Aprendizaje | 5 |

**RF-49** Cada puesto puede además tener su propia repartición de puntos, distinta de la
anterior.

**RF-50** Una persona puede ajustar cualquier nota que puso la IA. Queda registrado quién la
cambió, cuándo y por qué.

**RF-51** Vienen doce pruebas ya cargadas: Director, Coordinador de Operaciones, Talento,
Crecimiento, Compra de medios, Desarrollador, Diseñador, Editor de video, Ventas,
Seguimiento de clientes y Producto.

**RF-52** El cliente puede modificar esas pruebas o crear otras nuevas desde el sistema.

**RF-53** La pantalla para editar una prueba muestra todo en un solo lugar: enunciado,
tiempos, cambio inesperado y reparto de puntos. Nada de esto exige programar.

```
  PRUEBA · Desarrollador web

  Enunciado
  +----------------------------------------------+
  | Construye un sistema mínimo de agenda:       |
  | el cliente elige fecha y hora, registra sus  |
  | datos, recibe confirmación...                |
  +----------------------------------------------+

  Tiempos
    Duración total        [ 120 ] min
    El cambio aparece a los [  84 ] min   (70% — sugerido)
    Tiempo tras el cambio [  20 ] min

    |------------------------------------|------|
     0                                  84    120
     trabaja                            cambio

  Cambio inesperado
  +----------------------------------------------+
  | Ahora cada horario admite máximo 5 personas  |
  | y debe mostrar los cupos disponibles.        |
  +----------------------------------------------+

  Reparto de puntos                    suma: 100
    Funcionalidad          [ 25 ]
    Comprensión            [ 10 ]
    Arquitectura           [ 10 ]
    ...
                                    [ Guardar ]
```

**RF-54** La barra de tiempo se mueve al cambiar los números, para que se vea dónde cae el
cambio dentro de la prueba.

**RF-55** El sistema avisa si el reparto de puntos no suma 100, pero **deja guardar igual**.
Es un aviso, no un bloqueo: el cliente puede estar a mitad de un ajuste.

**RF-56** Los cambios en una prueba **no afectan a quien ya la rindió**. Cada entrega queda
atada a la versión con la que se hizo.

---

# 6. Etapa 4 — Simulación de 2 horas

**RF-57** El reclutador crea sesiones con fecha, hora, lugar y cupo. **No hay límite de
sesiones**: puede publicar una, dos o las que necesite.

**RF-58** Cada sesión indica **para qué vacantes sirve**. Puede ser para una sola, para
varias o para todas las abiertas.

Así se resuelve el caso real: si hay una sesión pensada solo para Desarrollador, los
candidatos de las otras vacantes no la ven.

```
  SESIÓN · Martes 3 sep, 9:00        Cupo 12 · quedan 4
  Sala principal
  Para: Desarrollador

  SESIÓN · Jueves 5 sep, 9:00        Cupo 20 · quedan 20
  Sala principal
  Para: Director de Operaciones · Coordinador
```

**RF-59** El candidato ve **solo las sesiones de su vacante** que todavía tengan cupo, y elige
una.

**RF-60** Si no hay ninguna sesión disponible para su vacante, el candidato queda esperando y
el reclutador lo ve en su pantalla de inicio como algo por resolver.

**RF-61** Cuando una sesión llena su cupo, deja de ofrecerse. El reclutador puede ampliar el
cupo o publicar otra fecha.

**RF-62** Una sesión se puede cancelar. A los candidatos que ya la habían elegido se les avisa
y vuelven a quedar pendientes de elegir otra.

**RF-63** Es una sesión **grupal en sala**, pero cada candidato trabaja en su propia pantalla
dentro del sistema.

**RF-64** La sesión tiene seis momentos cronometrados:

| Momento | Minutos | Qué pasa |
|---|---|---|
| Contexto | 0–10 | Lee la meta, los recursos y los límites |
| Preguntas | 10–20 | Puede preguntar; el sistema anota qué pregunta y qué no |
| Ejecución | 20–85 | Trabaja |
| Cambio inesperado | 85–105 | Cambia una condición o aparece un bloqueo |
| Entrega | 105–115 | Sube evidencia y explica |
| Conversación | 115–120 | Una persona conversa con él |

**RF-65** El sistema registra automáticamente la hora de: inicio, primera pregunta, inicio
del trabajo, primera evidencia, aparición del cambio, primera reacción al cambio, detección
de un bloqueo, aviso de ese bloqueo, entrega y autocrítica final.

**RF-66** El sistema genera **5 preguntas personalizadas** para la conversación final, a
partir de las contradicciones detectadas en toda la evaluación.

Ejemplo: *"En tu evaluación dijiste que avisas los riesgos temprano. Aquí detectaste el
bloqueo a las 10:41 y lo informaste a las 10:49. ¿Qué pasó?"*

**RF-67** La simulación se califica sobre 100 en diez aspectos: comprensión 10, calidad de
las preguntas 10, velocidad de arranque 10, priorización 10, ejecución 15, calidad 15,
autonomía 10, aviso preventivo 10, uso de recursos 5, evidencia y aprendizaje 5.

**RF-68** El sistema registra quién asistió y quién no.

---

# 7. Etapa 5 — Validación de 7 días

**RF-69** El candidato trabaja siete días y el sistema abre un expediente para registrar
cómo le fue.

**RF-70** Se cargan nueve métricas:

| Métrica | Peso |
|---|---|
| Resultado logrado | 25% |
| Calidad al primer intento | 15% |
| Velocidad | 10% |
| Confiabilidad | 10% |
| Autonomía | 10% |
| Aviso preventivo | 10% |
| Aprendizaje entre la primera y la segunda vez | 10% |
| Servicio | 5% |
| Aporte al sistema | 5% |

**RF-71** Las métricas se cargan a mano. Quién puede hacerlo (el jefe del área, Recursos
Humanos o ambos) es **configurable**.

**RF-72** Si la evaluación predijo alto pero el trabajo real fue bajo, **manda el trabajo real**.

---

# 8. La decisión

**RF-73** La decisión final **no es un promedio**. Es un semáforo:

| Resultado | Qué significa | Qué se hace |
|---|---|---|
| **Verde** | Evidencia consistente, sin fallos graves | Avanza o se contrata |
| **Ámbar** | Buen potencial, pero hay una contradicción | Se diseña una prueba específica |
| **Rojo** | Falla algo indispensable para el puesto | No avanza en ese puesto |
| **Sin datos** | No hay evidencia suficiente | Se pide otra prueba, no se asume que falla |

**RF-74** Existen fallos que **ninguna nota alta compensa**:

| Nivel | Qué no se perdona |
|---|---|
| Dirección | Falta grave de honestidad; no sabe priorizar; no controla; no decide |
| Supervisión | No sabe hacer seguimiento; avisa tarde; trata igual a todos; trabaja desordenado |
| Ejecución | Prueba deficiente; repite errores tras la corrección; esconde bloqueos |

**RF-75** La IA detecta esos fallos y explica en qué se basa. **Una persona los confirma**
antes de que bloqueen a nadie.

**RF-76** El sistema muestra un indicador de **cuánta evidencia respalda** cada perfil, para
distinguir a quien fue evaluado a fondo de quien apenas dejó rastro.

**RF-77** El **Bar Raiser** —alguien ajeno al área— puede revisar el expediente y dejar su
opinión registrada. **No puede bloquear** la contratación.

**RF-78** Cualquier persona con permiso puede cambiar una decisión del sistema, en cualquier
dirección. Debe justificarlo y queda registrado.

---

# 9. Seguimiento después de contratar

**RF-79** A los 30, 90 y 180 días el sistema registra cómo le fue realmente a la persona.

**RF-80** Esos datos vienen de RENASER OS.
⚠️ Depende de un módulo que hoy no existe.

**RF-81** El sistema compara la nota que sacó al ingresar contra su desempeño real.

**RF-82** Con esa comparación, Dirección puede ver qué preguntas no distinguen a los buenos
de los malos, y qué pesos no predicen nada.

---

# 10. Panel de gestión

**RF-83** Al entrar, el reclutador ve primero **lo que está frenado esperándolo**: candidatos
en zona dudosa, fallos graves por confirmar, sesiones sin programar. Debajo, sus vacantes con
cuánta gente hay en cada etapa.

**RF-84** La ficha del candidato muestra arriba el resumen —nota, semáforo, fortalezas,
riesgos, alineación personal— y debajo cada etapa se despliega para ver respuesta por
respuesta y por qué la IA puso cada nota.

**RF-85** El panel muestra cuántos días lleva cada candidato sin avanzar.

**RF-86** Dirección ve cuatro métricas:
1. Cuánta gente postuló, cuánta llegó a cada etapa y dónde se cae más.
2. Cuántos días toma el proceso completo y dónde se pierde el tiempo.
3. La nota de ingreso comparada con el desempeño real.
4. Qué porcentaje de decisiones tomó la IA sin intervención humana.

---

# 11. Avisos

**RF-87** El sistema manda correos automáticos cuando: se recibe la postulación, hay algo
pendiente en el portal, el candidato avanza de etapa, hay que elegir fecha de sesión, o no
continúa en el proceso.

**RF-88** El correo **nunca contiene las preguntas ni las pruebas**. Solo avisa y lleva al
portal.

**RF-89** A quien no continúa se le manda un correo breve de agradecimiento, **sin explicar
motivos y sin dar su nota**. Se le indica que sus datos quedan guardados para futuras
convocatorias.

**RF-90** Ese resultado se ve en tres lugares: el correo, el portal del candidato y el panel
de gestión.

**RF-91** Todo lo enviado queda registrado: a quién, cuándo y qué decía.

---

# 12. Administración

**RF-92** Las preguntas viven en la base de datos, no en el código. Hay una pantalla para
verlas y editarlas: enunciado, opciones, puntaje de cada opción, dimensiones que mide.

**RF-93** El banco se publica por versiones. Cada candidato queda atado a la versión que
respondió.

**RF-94** Si se cambian los pesos o las preguntas, **las notas anteriores no se recalculan**.
Así se puede reproducir cualquier decisión tal como se tomó.

**RF-95** Las pruebas del puesto se administran igual: plantillas editables, y se pueden
crear nuevas.

**RF-96** Los pesos de cada etapa y las notas mínimas se configuran por nivel de puesto y por
vacante.

**RF-97** Los roles son **configurables**. Al inicio son cuatro: Candidato, Reclutador, Jefe
del área y Dirección. Dirección tiene la mayor jerarquía: ve y hace todo en el sistema.

Quién puede hacer qué está definido acción por acción en
[Roles y permisos](04-ROLES-Y-PERMISOS.md).

**RF-98** Un rol es un **conjunto de permisos con nombre**, guardado en la base de datos. Los
nombres y permisos se cambian desde una pantalla, sin tocar código ni desplegar, porque
probablemente terminen alineándose con los roles que ya existen en RENASER OS.

**RF-99** Dirección administra los roles desde un panel: crea roles, marca permisos y se los
asigna a las personas. Con tres protecciones:

- **Nadie puede dejarse fuera.** Dirección no edita su propio rol, y el sistema no deja
  guardar si el cambio dejaría a nadie con permiso de administrar roles.
- **Los permisos se agrupan por área** y se describen en lenguaje normal. Hay un botón para
  ver el sistema como ese rol antes de guardar.
- **Si un cambio afecta a alguien trabajando**, el mensaje lo dice claro y su trabajo no
  desaparece de la pantalla.

Hay cosas que **nunca** son configurables: que un candidato vea a otros candidatos, que las
claves lleguen al portal, que se borre la auditoría, que se salte el consentimiento, o que la
inteligencia artificial contrate sin una persona.

El detalle está en [Roles y permisos](04-ROLES-Y-PERMISOS.md).

**RF-100** Toda acción que cambie una decisión queda registrada: quién, cuándo, qué cambió y
por qué. Los cambios de permisos también.

---

# Qué cambia sin programar

Renaser ajusta seguido sus criterios. Todo lo de esta lista se cambia desde una pantalla, sin
tocar código y sin volver a publicar el sistema:

| Qué | Quién |
|---|---|
| Las preguntas: texto, opciones, puntajes | Reclutador prepara · Dirección publica |
| Las pruebas: enunciado, tiempos, cambio inesperado, puntos | Reclutador prepara · Dirección publica |
| Los pesos de cada etapa | Dirección |
| Las notas mínimas | Reclutador |
| El tamaño de la zona dudosa | Dirección |
| Los textos de los correos | Reclutador |
| Las instrucciones que recibe la inteligencia artificial | Dirección |
| Los roles y sus permisos | Dirección |
| Cuántos días sin avanzar antes de cerrar una postulación | Dirección |
| Cuántas veces puede repetirse el ámbar | Dirección |

**Cambiar algo no altera lo ya evaluado.** Cada candidato queda atado a la versión con la que
se le evaluó, así que su nota nunca cambia sola después de habérsela comunicado.

---

# Documentos relacionados

Este documento se lee solo. Estos otros existen para el detalle largo:

| Documento | Qué contiene |
|---|---|
| [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) | Tecnología, seguridad, rendimiento, datos personales |
| [Estados de la postulación](03-ESTADOS-POSTULACION.md) | Los 24 estados y cómo se pasa de uno a otro |
| [Roles y permisos](04-ROLES-Y-PERMISOS.md) | Quién puede hacer qué, acción por acción |
| [Embudo de selección](diagramas/embudo-seleccion.html) | Las cinco etapas, en un dibujo |
| [Estados, en un dibujo](diagramas/estados-postulacion.html) | El ciclo que se repite en cada etapa |

Los documentos originales de Renaser están en `00-INSUMOS/`. El vigente es
**Sistema_Completo_Talento_RENASER_Seleccion_2026_2029**; los otros dos quedaron atrás.

---

# Pendiente de Renaser

| Qué falta | Bloquea |
|---|---|
| Comprar la prueba psicométrica | Su 30% se reparte mientras tanto. No bloquea |
| Retention Fit y presentación realista del puesto | Es una pantalla más. Falta definir qué mide |
| Elegir el modelo de inteligencia artificial | No bloquea el diseño |
| Definir la figura legal de los 7 días de trabajo | Bloquea esa etapa |
| Confirmar que el CV sí descarta | Contradice lo que el cliente dijo el 08/08 |
| Duración de dos pruebas: Coordinador de Operaciones y Compra de medios | No bloquea: van con valor propuesto y son editables |
| En qué minuto aparece el cambio inesperado en cada prueba | No bloquea: se calcula al 70% y es editable |

**Ninguno de los tres últimos frena el desarrollo.** El sistema arranca con valores razonables
y Renaser los ajusta desde la pantalla cuando lo tenga claro. Esa es la razón de hacerlos
editables: el cliente todavía no tiene la respuesta, y probablemente la cambie más de una vez.
