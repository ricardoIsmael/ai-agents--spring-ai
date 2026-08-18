# Curso del backend · plan de estudio

Plan para entender **todo** este backend leyendo el código que ya existe. No es documentación
del producto: es la ruta para aprenderlo, en orden, sin saltos.

Nivel de partida: Java básico y Spring Boot básico. Los fundamentos que hagan falta
(inyección de dependencias, JPA, filtros, transacciones) se explican cuando aparecen, sobre el
código real, no antes.

---

## Qué hay realmente aquí

Contado sobre la rama `feat/talentov2`, no sobre los documentos:

| | |
|---|---|
| Clases Java | **300**: 113 en `ai/` (motor de agentes) y 187 en selección de personal |
| Migraciones | V1 a V14. Crean **63 tablas**: 35 del hito 1 y 28 del hito 2 |
| Estados de postulación | **18**, sembrados en V9 |
| Roles | **5**: candidato, talento, responsable de área, dirección, administrador |
| Permisos | **44**: 30 del hito 1 (V9), 13 del hito 2 (V12) y 1 suelto (V13) |
| Controladores | 12 nuestros, 4 del motor de agentes |
| Tests | 8 archivos: unitarios, migraciones con Postgres real y dos flujos completos |

Dominios más grandes de selección: `perfilintegral` (67 clases, el hito 2), `seguridad` (16),
`postulacion` (15), `vacante` (14), `pesos` (13).

⚠️ Dos avisos que cambian lo que hay que creer:

- Los documentos hablan de **93 tablas y 73 permisos**: eso es el sistema completo, no lo
  construido. Cuando un documento y una migración se contradigan, **manda la migración**.
- `CLAUDE.MD` dice que la frontera con el motor de agentes son cinco servicios. Hoy son
  **diez**, más dos clases de `comun`. Creció con el hito 2.

---

## Cómo es cada lección

Siempre la misma forma:

1. **Qué problema resuelve** — en una frase, sin palabras técnicas.
2. **El recorrido** — el código real, archivo por archivo, con enlaces a la línea.
3. **Una pregunta** — la respondes tú. Si no sale, volvemos atrás; no seguimos.
4. **Qué romper** — un cambio pequeño que hace fallar algo, para ver quién sostiene qué.

Las lecciones 1 a 12 son la **pista A** (selección de personal). La 13 es la **pista B**
(motor de agentes, de Ricardo, solo lectura). Van separadas porque no comparten arquitectura y
mezclarlas confunde las dos.

---

## Pista A · La selección de personal

### 1. El mapa: dos proyectos en un repositorio

Por qué hay 300 clases que casi no se hablan. Los dominios, qué guarda cada uno, y la frontera
real: `ai/exception/ResourceNotFoundException` usada en diez servicios, y un controlador del
hito 2 que vive del lado de Ricardo (`AgentesIaPanelController`). Las dos clases que enumeran
controladores a mano y hay que tocar al añadir uno: `ManejadorErrores` y `ConfiguracionSwagger`.

### 2. El arranque: qué pasa antes de la primera petición

`AiEngineApplication`, `application.yaml`, `docker-compose.yml` (Postgres en 5433, RabbitMQ).
Qué es un bean y quién los crea. Por qué la aplicación **no arranca sin `DEEPSEEK_API_KEY`**
aunque la selección de personal no use IA. Dónde viven los secretos.

### 3. Los datos primero: Flyway manda

Las catorce migraciones en orden y la regla dura: **una migración aplicada no se edita jamás**,
se escribe otra. `ddl-auto: validate`: JPA no crea ni cambia tablas, solo comprueba que las
entidades cuadren. Las semillas de V9 y lo que añaden V12–V14.

Qué romper: cambia un campo de una entidad sin migración y mira el error de arranque.

### 4. Una petición de punta a punta

**La lección central.** Un endpoint simple del panel, seguido entero:

```
petición → filtro → controlador → DTO de entrada → servicio (interfaz)
        → implementación → repositorio → entidad → base de datos
        → mapper → DTO de salida → respuesta
```

Qué hace cada capa, por qué existe y qué pasa si la quitas. Aquí entran los fundamentos:
inyección de dependencias, `@Transactional`, qué es un DTO y por qué no se devuelve la entidad.
Todo lo demás es una variación de este recorrido.

### 5. Seguridad · las dos puertas

Dos formas de entrar —el candidato con cuenta propia, el equipo con el token de RENASER OS— y
por qué no se parecen. `FiltroIdentidad`, `ServicioToken`, `ServicioContexto`, y qué es un
filtro frente a un controlador.

### 6. Seguridad · permisos con alcance

Un rol no está en el código: es un conjunto de permisos en la base de datos. `Permisos.alcanceDe`
y lo que casi todos entienden mal: **el alcance se aplica dentro de la consulta**, no filtrando
en Java después. `TODO` frente a `PROPIO`.

Qué romper: quita el alcance de una consulta y mira a un candidato viendo postulaciones ajenas.

### 7. La máquina de estados

Los 18 estados como rejilla —cinco etapas por cuatro momentos, más tres finales—, por eso el
siguiente estado se **calcula**. `MaquinaEstados.transicionar` como único camino, y el historial
que no se borra. Lo leemos junto a `MaquinaEstadosTest`, que es la especificación ejecutable.

### 8. Los dominios, en el orden del negocio

`solicitud` (qué falta en la empresa) → `vacante` + `pesos` (qué se busca y cuánto vale cada
etapa) → `postulacion` (la persona avanzando) → `portal` (todo lo que ve el candidato).

### 9. El Perfil Integral · el hito 2

El dominio más grande de selección, 67 clases, y lo que estás construyendo ahora: banco de
preguntas con versiones, plantillas de evaluación, la evaluación que responde el candidato, la
calificación y las alertas. Por qué todo va versionado y nada se recalcula hacia atrás.

### 10. Lo transversal

`archivo` (almacén en disco, enlaces firmados), `notificacion` (el correo se **registra pero no
se envía**), `auditoria` (registro que no se modifica), `parametro`, `consentimiento`,
`catalogo`, `administracion`.

### 11. Errores y contratos

`ManejadorErrores`: por qué enumera controladores a mano y por qué tiene precedencia máxima.
Coexisten **dos rutas de error** en el mismo proceso: la nuestra y el `GlobalControllerAdvice`
de agentes. `ProblemDetail` y qué ve el frontend. Swagger y el candado.

### 12. Los tests

`MigracionesIT` levanta un Postgres real con Testcontainers. `FlujoHito1IT` y `FlujoEvaluacionIT`
recorren el sistema entero de una pieza: la forma más rápida de verlo funcionar sin navegador.
Cómo se corren y cómo leer un fallo.

---

## Pista B · El motor de agentes

### 13. `ai/` de un vistazo

Solo lectura: no se toca, pero comparte proceso, base de datos y puerto. Los 15 agentes y su
respuesta estructurada, `FlowController`, el encadenamiento por RabbitMQ con `routing[]` y tope
de profundidad, los prompts en `resources/prompts/`, y el RAG con pgvector más Ollama.

---

## Cierre · qué falta en el sistema

Saber qué **no** existe es parte de entenderlo.

| Hueco | Estado |
|---|---|
| La identidad del equipo es un `dev-login` | El contrato con RENASER OS no existe aún |
| El correo se registra pero no sale | Falta el dominio de Renaser |
| Hito 3 (prueba del puesto, simulación, validación) | Definido en [Alcance del MVP](08-ALCANCE-DEL-MVP.md), sin construir |

---

## Ritmo

Una lección por sesión. La 4 es la más importante y puede llevar dos. Las 5 y 6 van juntas o
ninguna se entiende. En cada lección abres el archivo y al final rompes algo.
