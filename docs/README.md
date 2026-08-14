# Documentación · Sistema de selección Renaser

Backend en Java + Spring Boot para el módulo de selección de personal de Renaser Consulting,
integrado al frontend Next.js de RENASER OS.

---

## Por dónde empezar

**Lee primero [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md).** Se lee solo: trae el
objetivo, el alcance, las cinco etapas y dónde interviene una persona. Con ese documento se
entiende el sistema completo.

---

## Los documentos

| Documento | Qué contiene |
|---|---|
| [01 · Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) | RF-01 a RF-100. Qué hace el sistema |
| [02 · Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) | RNF-01 a RNF-66. Tecnología, seguridad, rendimiento |
| [03 · Estados de la postulación](03-ESTADOS-POSTULACION.md) | Los estados de una postulación y sus transiciones |
| [04 · Roles y permisos](04-ROLES-Y-PERMISOS.md) | Quién puede hacer qué, acción por acción |
| [05 · Modelo de datos](05-MODELO-DE-DATOS.md) | Las 71 tablas por área y por qué el modelo es así. Se lee |
| [06 · Inventario de pantallas](06-INVENTARIO-DE-PANTALLAS-MOCKUPS.md) | Las 21 pantallas base, estados, ventanas, campos y datos exactos de los mockups |
| [07 · Diccionario de datos](07-DICCIONARIO-DE-DATOS.md) | Cada tabla con todas sus columnas, tipos y claves. Se consulta |

### Diagramas

Archivos HTML que se abren en el navegador:

- [Embudo de selección](diagramas/embudo-seleccion.html) — las cinco etapas con sus pesos
- [Estados de la postulación](diagramas/estados-postulacion.html) — el ciclo que se repite
- [Modelo de datos](diagramas/modelo-de-datos.html) — el mapa de la base de datos

### Mockups

Prototipos de pantalla. **Los mantiene otra persona**, no se editan desde aquí:

- [Panel de gestión](mockups/renaser-os-reclutamiento.html) — la vista del equipo
- [Portal del candidato](mockups/portal-candidato.html) — la vista pública

### Insumos

Material de origen. Solo se consulta:

| Archivo | Qué es |
|---|---|
| `Sistema_Completo_Talento_RENASER_Seleccion_2026_2029.docx` | **Documento vigente del cliente** |
| `Banco_Maestro_Preguntas...docx` | Las 200 preguntas con sus claves |
| `Sistema_RENASER_Talent_Intelligence...docx` | Versión anterior, descartada |
| `ANALISIS-DOCUMENTOS.md` | Qué documento manda sobre cuál y por qué |
| `DECISIONES.md` | Decisiones tomadas durante el análisis |
| `NOTAS-TEMPORALES.md` | Lo que sigue pendiente |
| `entrevista-cliente-2026-08-08.md` | Transcripción de la reunión |

---

## El sistema en corto

Un candidato postula en el portal público de Renaser y atraviesa cinco etapas:

| Etapa | Qué pasa | Quién califica |
|---|---|---|
| 1 · CV | La IA lo puntúa sobre 100 | IA |
| 2 · Evaluación integral | 126 a 138 preguntas | IA |
| 3 · Prueba del puesto | Cronometrada, con cambio inesperado | IA |
| 4 · Simulación de 2 h | Sesión grupal, cada uno en su pantalla | Persona |
| 5 · Validación de 7 días | Trabajo real | Persona |

Al final, una decisión: **verde** (contrata), **ámbar** (falta averiguar algo) o **rojo**
(no pasa).

---

## Con qué está hecho

| | |
|---|---|
| Backend | Java 25 con Spring Boot 4.1 |
| Base de datos | PostgreSQL propio, con pgvector |
| Trabajo en segundo plano | RabbitMQ |
| Inteligencia artificial | Ollama, en el propio servidor |
| Frontend | Next.js, el de RENASER OS |

Todo corre en servidores de Renaser. Los datos de los candidatos no salen a servicios de
terceros, y el modelo de inteligencia artificial tampoco.

---

## Antes de programar

Cuatro cosas que definen el diseño y conviene tener presentes:

**Los estados mandan.** Ningún estado fuera de la lista del documento 03 puede existir. Cada
cambio de estado se guarda como un registro aparte que no se modifica ni se borra.

**Casi todo es configurable.** Preguntas, pruebas, tiempos, pesos, notas mínimas, roles y
textos de correo viven en la base de datos, no en el código. El cliente cambia estas cosas
seguido.

**Nada se recalcula hacia atrás.** Cada candidato queda atado a la versión de preguntas y
pesos con la que se le evaluó. Su nota nunca cambia sola.

**Los permisos se verifican en el servidor.** Ocultar un botón no es seguridad. Cada llamada
a la API comprueba quién es el usuario y si puede hacer eso.

---

## Pendiente del cliente

| Qué falta | Bloquea |
|---|---|
| Figura legal del periodo de 7 días | **Sí**, esa etapa |
| Confirmar que el CV descarta | Contradice lo que dijo en la reunión del 08/08 |
| Comprar la prueba psicométrica | No: su 30% se reparte mientras tanto |
| Cuánto tiempo se conservan los datos de un candidato | No: pero la ley 29733 lo exige |
| Qué máquina sostiene el modelo de inteligencia artificial | No en diseño; sí al construir |
| Retention Fit y presentación del puesto | No: es una pantalla más |
