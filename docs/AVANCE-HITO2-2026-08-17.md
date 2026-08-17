# Avance del hito 2 — 17/08/2026

Resumen de lo que se construyó del hito 2 ("Que la máquina ordene") en esta sesión. El
hito 1 ya estaba hecho antes de empezar. Este documento es solo un resumen — el detalle
completo del modelo de datos sigue viviendo en [07-DICCIONARIO-DE-DATOS.md](07-DICCIONARIO-DE-DATOS.md)
y el alcance en [08-ALCANCE-DEL-MVP.md](08-ALCANCE-DEL-MVP.md).

## En una frase

**La base de datos y la administración del hito 2 ya están hechas y probadas. Lo que
falta es la calificación real con inteligencia artificial, y esa parte está bloqueada
por algo que tiene que entregar Renaser, no por código.**

---

## Lo que ya funciona

### 1. Las 28 tablas nuevas (base de datos)

Todo lo que el hito 2 necesita para guardar datos: el banco de preguntas, las plantillas
de evaluación, la evaluación de un candidato, el Perfil de Talento con sus hallazgos, y
los agentes de IA con su historial de ejecuciones. Se aplicó contra la base real y quedó
probado con la suite de tests completa.

### 2. Cuatro paneles de administración, ya usables

Un administrador (Talento o Dirección, según el permiso) ya puede, desde hoy, por la API:

| Qué se administra | Quién puede |
|---|---|
| **Banco de preguntas** — crear versiones, agregar preguntas y sus opciones, publicar | Talento y Dirección |
| **Plantillas de evaluación** — la "receta" de cuántas preguntas de cada tipo le tocan a un puesto | Talento y Dirección |
| **Pesos** — cómo se reparte la nota entre etapas, entre CV/psicométrico/evaluación, y entre las dimensiones y criterios de cada nivel de puesto | Solo Dirección |
| **Agentes de IA** — los 9 agentes y sus instrucciones, con versión e historial | Solo Dirección |

Cada uno de estos cuatro tiene sus endpoints reales, con su permiso, y quedó probado a
mano contra la base de datos real (no son solo tablas vacías: se creó, publicó y validó
un caso de cada uno).

### 3. Una regla que estaba solo escrita, ahora la aplica el sistema

El diccionario de datos decía "que los pesos sumen 100 lo comprueba el código al
publicar" pero esa comprobación nunca se había programado, ni siquiera para el hito 1.
Ahora sí: si alguien intenta publicar una versión de pesos donde las partes no suman lo
que deben, el sistema lo rechaza con un mensaje claro, no lo deja pasar.

---

## Lo que falta: la Fase C

Esto es lo único que queda del hito 2, y es la parte que **de verdad usa inteligencia
artificial para calificar a un candidato**: que la IA puntúe el currículum, puntúe las
respuestas abiertas, detecte contradicciones, y arme el Perfil de Talento con sus cuatro
grupos de prioridad.

### Por qué no se hizo todavía

No es una decisión técnica, es que **falta un insumo que tiene que entregar Renaser**:
los pesos reales de los criterios de al menos una prueba del puesto. Hoy esos criterios
son una lista de 10 a 12 sin números (ver "Paso 0" en
[08-ALCANCE-DEL-MVP.md](08-ALCANCE-DEL-MVP.md)). Programar la calificación ahora
significaría inventar esos números, y probablemente haya que rehacerlo cuando Renaser
entregue los reales.

### Qué hace falta para poder construirla

1. Que Renaser le ponga peso a los 10-12 criterios de al menos una prueba real (es el
   mismo insumo del "Paso 0", una tarde de trabajo de su parte).
2. Con eso, construir: el agente de IA que califica (reutilizando el motor de IA que ya
   existe para el resto del sistema, no uno nuevo), el cálculo del grupo de prioridad, y
   la confirmación por lote para el equipo.

---

## Dónde está el código

Todo lo nuevo vive en dos sitios:

- `com.renaser.ai.ai_engine.perfilintegral` — las tablas y la administración propias del
  hito 2 (banco de preguntas, plantillas, evaluación, Perfil de Talento).
- `com.renaser.ai.ai_engine.pesos` — la administración de pesos (ya existían las tablas
  del hito 1, se les agregó la administración que nunca tuvieron).
- `com.renaser.ai.ai_engine.ai.controller.perfilintegral` — la administración de agentes
  e instrucciones, deliberadamente separada del resto del motor de agentes de Ricardo
  para no tocar su manejo de errores ni su candado de Swagger por accidente.

Las migraciones nuevas son `V10` a `V13` en `src/main/resources/db/migration/` (V1 a V9
son del hito 1, no se tocaron).

## Cómo probarlo

1. Levantar la app (`./mvnw.cmd spring-boot:run`, necesita Postgres y RabbitMQ de
   `docker-compose.yml` corriendo).
2. Entrar con `POST /api/v1/panel/auth/dev-login`.
3. Los endpoints nuevos están documentados en Swagger (`/swagger-ui.html`), agrupados
   como "Panel · Banco de preguntas", "Panel · Plantillas de evaluación", "Panel ·
   Pesos" y "Panel · Agentes de IA".
