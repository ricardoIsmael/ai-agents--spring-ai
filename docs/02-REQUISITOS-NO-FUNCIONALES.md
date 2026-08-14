# Requisitos no funcionales

Sistema de selección de personal — Renaser Consulting
Versión 1.1 · 2026-08-14

Este documento dice **cómo** debe funcionar el sistema: con qué se construye, qué tan rápido,
qué tan seguro y qué pasa cuando algo falla.

El **qué hace** está en [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md).

---

## El sistema en una página

Un portal público donde la gente postula, y un panel dentro de RENASER OS donde el equipo de
Renaser trabaja. Entre los dos, cinco etapas de evaluación que van de leer un currículum a
verla trabajar siete días.

```
   CANDIDATO                      EQUIPO RENASER
   portal público                 panel en RENASER OS
        |                                |
        +----------------+---------------+
                         |
                    Next.js
                         |
                   API REST / JSON
                         |
                   Spring Boot
                         |
        +----------------+----------------+
        |                |                |
   PostgreSQL         RabbitMQ      Almacén de
   (propio)           (la cola)      archivos
                         |
                    Ollama (califica)
```

Lo que tarda —puntuar un CV, corregir una prueba— no se hace mientras el candidato espera.
Se deja en una cola y se atiende aparte. Por eso hay una cola en el dibujo.

### Las decisiones que ya están tomadas

| | |
|---|---|
| Backend | Java 25 con Spring Boot 4.1 |
| Frontend | Next.js, el que ya existe |
| Base de datos | PostgreSQL propio, con la extensión pgvector |
| Archivos | Almacén propio, fuera de la base de datos |
| Cola de trabajo en segundo plano | RabbitMQ |
| Comunicación con el frontend | API REST con JSON |
| Modelo de inteligencia artificial | **Ollama**, corriendo en el propio servidor |
| Correo | Desde el dominio propio de Renaser |

El modelo de inteligencia artificial corre **dentro del servidor de Renaser**, no en un
servicio de otra empresa. Eso resuelve de entrada el problema legal más incómodo: los datos
de los candidatos no salen a ninguna parte. También quita el gasto por consulta, y a cambio
exige una máquina que aguante el trabajo.

### Cuánta gente lo va a usar

| | |
|---|---|
| Vacantes abiertas a la vez | Hasta 10 |
| Postulantes por vacante | Hasta 500 |
| Usuarios internos | Menos de 20 |
| Candidatos a la vez en la simulación | Hasta 50 |
| Contrataciones al año | Decenas, no cientos |

Es una empresa pequeña que contrata de forma continua. No hace falta diseñar para miles de
usuarios simultáneos, pero sí dejar el sistema separado por partes para poder crecer sin
rehacerlo.

---

# 1. Tecnología

**RNF-01** El backend se construye en **Java con Spring Boot**.

**RNF-02** El frontend es el proyecto **Next.js** de RENASER OS que ya existe.

**RNF-03** Los dos se comunican por **API REST con JSON**. Son proyectos separados: cada uno
se despliega por su cuenta y el contrato entre ellos es la API.

**RNF-04** La base de datos es **PostgreSQL propio**, con la extensión **pgvector** instalada.
Para desarrollar se levanta en un contenedor; en producción, en el servidor de Renaser.

**RNF-05** Los archivos se guardan **fuera de la base de datos**. La base solo guarda la ruta.

## Reglas de la base de datos y los archivos

Cinco reglas que evitan problemas conocidos:

**RNF-06** **Spring Boot es el dueño de la identidad.** Los usuarios, las contraseñas y los
permisos son tablas suyas. No se delega la seguridad en la base de datos ni en ninguna capa
intermedia.
*Por qué:* tener permisos en dos capas distintas hace que ninguna de las dos sea confiable.

**RNF-07** La base de datos **nunca se expone a internet**. Solo la alcanza el backend, desde
la red interna del servidor.
*Por qué:* es la única copia de los datos personales de todos los candidatos. Un puerto
abierto de PostgreSQL se encuentra con un escaneo automático en cuestión de horas.

**RNF-08** La estructura de la base se maneja con **Flyway**, en archivos versionados dentro
del repositorio. Nadie crea ni modifica tablas a mano.
*Por qué:* si el esquema cambia por fuera, deja de coincidir con lo que espera el backend y
el problema aparece en producción.

**RNF-09** El almacén de archivos es **privado**. El acceso se da con enlaces firmados de
corta duración que genera el backend cuando alguien abre el archivo.
*Por qué:* un CV contiene nombre, teléfono e historial laboral. Un almacén público lo expone
a cualquiera que tenga el enlace.

**RNF-10** Todo se aloja en **servidores propios de Renaser**, en Perú si es posible. Los
datos de los candidatos no salen a servicios de terceros, y el modelo de inteligencia
artificial corre en la misma máquina.

---

# 2. Datos personales

**RNF-11** El sistema cumple la **Ley 29733** de protección de datos personales del Perú.

**RNF-12** El candidato acepta el tratamiento de sus datos al crear su cuenta. Se guarda
fecha, hora, versión del texto aceptado y dirección desde donde se aceptó.

**RNF-13** El texto de consentimiento dice **tres cosas** de forma clara:
1. Que sus datos se usan para evaluar su postulación.
2. Que **una inteligencia artificial participa** en esa evaluación.
3. Dónde se guardan sus datos y **cuánto tiempo**. Como todo corre en servidores de Renaser,
   no hay que declarar envío a otro país; el plazo de conservación sigue pendiente de fijar.

**RNF-14** El texto de consentimiento se versiona. Si cambia, los que ya aceptaron quedan
ligados a la versión que firmaron.

**RNF-15** El candidato puede **pedir que se borren sus datos**. El sistema debe poder
hacerlo sin romper los registros de auditoría.

**RNF-16** Antes de que la IA lea un CV, el sistema **oculta** foto, edad, sexo y estado
civil. Esa versión oculta es la única que se envía al modelo.

**RNF-17** El sistema guarda qué versión del CV se envió a la IA, para poder demostrar que la
regla se cumplió.

**RNF-18** Los datos de un candidato **no salen de Renaser** ni se le mete en un proceso al
que no postuló. Nadie ajeno a la empresa los ve, y nadie aparece como candidato de una
vacante sin haberse postulado a ella.

Distinto es lo que pasa **dentro de sus propias postulaciones**: si ya respondió las
preguntas de un nivel y postula a otro puesto del mismo nivel, sus respuestas se reutilizan y
no las repite. Eso es una comodidad para él, no un uso de sus datos a sus espaldas.

---

# 3. Seguridad

**RNF-19** Todo el tráfico va por HTTPS.

**RNF-20** Las contraseñas se guardan cifradas de forma irreversible. Nunca en texto plano.

**RNF-21** Cada llamada a la API verifica dos cosas: quién es el usuario y si tiene permiso
para eso.

**RNF-22** Un candidato solo puede ver **sus propias** postulaciones y respuestas.

**RNF-23** El jefe del área solo ve los candidatos de **sus** vacantes.

**RNF-24** Las claves de puntuación **nunca** se envían al portal del candidato. Ni siquiera
escondidas en el código de la página.
*Por qué:* si viajan al navegador, cualquiera puede leerlas y el banco de preguntas queda
inutilizado.

**RNF-25** Las contraseñas de la base de datos, de la cola y del correo van en variables de
entorno, nunca en el código.

**RNF-26** El sistema limita cuántas veces seguidas se puede intentar entrar, para frenar
ataques de fuerza bruta.

---

# 4. Auditoría

**RNF-27** Toda acción que cambie una decisión queda registrada: quién, cuándo, qué cambió,
qué valor tenía antes y por qué.

**RNF-28** Los registros de auditoría **no se pueden modificar ni borrar**.

**RNF-29** Cada nota calculada guarda con qué versión de preguntas y de pesos se calculó.

**RNF-30** El sistema puede **reproducir cualquier evaluación pasada** tal como se vio en su
momento: mismas preguntas, mismo orden, mismos pesos.

**RNF-31** Se guarda la respuesta completa de la IA, no solo la nota que produjo.
*Por qué:* si alguien reclama una calificación, hay que poder revisar en qué se basó.

**RNF-32** Se registra cuánto costó cada llamada a la IA, para poder controlar el gasto.

---

# 5. Inteligencia artificial

**RNF-33** El modelo es **Ollama corriendo en el propio servidor**. Aun así, la lógica de
evaluación no depende de él: cambiar de modelo, o pasar a uno de pago, no obliga a
reescribirla.
*Por qué:* que el modelo sea local es lo que permite prometerle al candidato que sus datos no
salen de Renaser. Y que sea intercambiable es lo que permite mejorar la calidad si el modelo
local se queda corto calificando.

**RNF-34** Los textos de instrucción que se envían a la IA se administran como configuración,
con versiones, y se pueden cambiar sin volver a desplegar el sistema.

**RNF-35** Si la IA no responde o falla, la calificación queda **pendiente** y se reintenta.
Nunca se guarda una nota inventada ni un cero.

**RNF-36** Si un dato no existe, la IA debe decir que **falta el dato**. Nunca inventarlo.

**RNF-37** Toda calificación de la IA viene con su explicación. Una nota sin explicación no
se acepta.

**RNF-38** El sistema tiene un tope de gasto mensual configurable. Al acercarse, avisa.

**RNF-39** Las calificaciones se procesan en segundo plano. El candidato no espera a que la
IA termine para seguir.

---

# 6. Rendimiento

**RNF-40** Las pantallas del panel cargan en menos de 2 segundos con hasta 500 candidatos por
vacante.

**RNF-41** Guardar una respuesta del examen tarda menos de 500 milisegundos.
*Por qué:* el candidato responde más de 100 preguntas seguidas. Si cada una demora, abandona.

**RNF-42** El sistema soporta **50 candidatos rindiendo la simulación al mismo tiempo**, ya
que es una sesión grupal.

**RNF-43** Calificar una evaluación completa con IA no debe tardar más de 10 minutos.

**RNF-44** Subir un archivo de hasta 200 MB debe funcionar sin cortarse.
*Por qué:* la prueba de edición de video entrega archivos pesados.

---

# 7. Disponibilidad y respaldo

**RNF-45** Durante una simulación de 2 horas el sistema **no puede caerse**. Es una sesión
presencial con varias personas y no se puede repetir.

**RNF-46** Si el sistema se cae durante un examen, el candidato retoma donde quedó. Nada de
lo respondido se pierde.

**RNF-47** La base se respalda a diario y los respaldos se guardan al menos 30 días.

**RNF-48** Se puede restaurar la base a un momento anterior.

---

# 8. Cronómetros

**RNF-49** El tiempo lo controla el **servidor**, no el navegador.
*Por qué:* si el reloj vive en la página, se puede manipular desde el navegador.

**RNF-50** Si el candidato cierra la página durante una prueba cronometrada, **el tiempo
sigue corriendo**.

**RNF-51** El cambio inesperado se dispara solo, al minuto configurado, aunque el candidato
no esté mirando la pantalla.

**RNF-52** Las horas registradas durante la simulación se guardan con precisión de segundos y
con zona horaria.

---

# 9. Usabilidad

**RNF-53** El portal funciona en celular, tablet y computadora. La postulación y las
preguntas deben poder responderse desde el teléfono.

**RNF-54** La prueba del puesto y la simulación están pensadas para computadora.

**RNF-55** Todo el texto que ve el candidato está en español simple, sin términos internos ni
palabras en inglés.

**RNF-56** El candidato ve en todo momento en qué etapa está y qué le toca hacer.

**RNF-57** Durante un examen largo se muestra el avance (por ejemplo, "pregunta 47 de 90").

**RNF-58** Los mensajes de error dicen qué hacer, no muestran detalles técnicos.

---

# 10. Mantenimiento

**RNF-59** El sistema se construye por partes independientes: vacantes, candidatos,
evaluación, calificación, avisos.
*Por qué:* así se puede cambiar una parte sin romper las demás.

**RNF-60** Lo que puede cambiar sin tocar código: preguntas, pruebas, pesos, notas mínimas,
roles, permisos, textos de correo, textos de instrucción de la IA, plazos de cierre
automático.

**RNF-61** El código y la documentación están en español, igual que el resto del proyecto.

**RNF-62** Las reglas de cálculo de notas tienen pruebas automáticas.
*Por qué:* un error ahí descarta gente injustamente y nadie se entera.

**RNF-63** Existe un entorno de pruebas separado del de producción, con datos falsos.

---

# 11. Escala

Las cifras están al inicio, en «El sistema en una página».

**RNF-64** No hace falta diseñar para miles de usuarios simultáneos. Pero la separación por
partes debe permitir crecer sin rehacer el sistema.

⚠️ **Sobre mejorar el sistema con el tiempo:** el documento de Renaser propone recalibrar los
pesos cada 30 a 50 contrataciones. A este ritmo, eso son varios años de datos. El sistema
**guarda** todo desde el primer día y **muestra** la comparación entre lo que predijo y lo
que pasó, pero la recalibración se hace **a mano**, mirando esos datos. No se construye un
ajuste automático que nunca tendría suficiente información para funcionar.

---

# 12. Lo que depende de otros

| De qué depende | Estado | Qué pasa si no está |
|---|---|---|
| Datos de desempeño de RENASER OS | **No existe** | El seguimiento a 30/90/180 días no se puede llenar solo |
| Prueba psicométrica | **No comprada** | Su 30% se reparte entre las otras dos partes |
| Servidor que aguante el modelo local | **Sin confirmar** | Sin máquina suficiente, calificar tarda de más |
| Dominio de correo de Renaser | **Sin confirmar** | Los correos pueden caer en spam |

**RNF-65** El correo sale desde el dominio propio de Renaser, con la configuración de
autenticación correcta.
*Por qué:* si una invitación a la sesión cae en spam, se pierde al candidato.

**RNF-66** Cada dependencia externa se construye de forma que el sistema **siga funcionando
sin ella**, aunque esa parte quede vacía.

---

# Documentos relacionados

| Documento | Qué contiene |
|---|---|
| [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) | Qué hace el sistema, etapa por etapa |
| [Estados de la postulación](03-ESTADOS-POSTULACION.md) | Los 24 estados y cómo se pasa de uno a otro |
| [Roles y permisos](04-ROLES-Y-PERMISOS.md) | Quién puede hacer qué, acción por acción |
| [Embudo de selección](diagramas/embudo-seleccion.html) | Las cinco etapas, en un dibujo |
| [Estados, en un dibujo](diagramas/estados-postulacion.html) | El ciclo que se repite en cada etapa |
