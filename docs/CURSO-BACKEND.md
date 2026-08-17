# Curso del backend · plan de estudio

Plan para entender **todo** este backend leyendo el código que ya existe. No es documentación
del producto: es la ruta para aprenderlo, en orden, sin saltos.

---

## Qué hay realmente aquí

Números contados sobre el código, no sobre los documentos:

| | |
|---|---|
| Clases Java | 208: **100** en `ai/` (motor de agentes) y **108** en selección de personal |
| Migraciones | V1 a V9. Crean **35 tablas**: 34 del hito 1 más `agent_run`, que es del motor de agentes |
| Estados de postulación | **18**, sembrados en V9 |
| Roles | **5**: candidato, talento, responsable de área, dirección, administrador |
| Permisos | **30** en el catálogo del hito 1, con **65** asignaciones a roles |
| Controladores | 6 de selección, 4 del motor de agentes |
| Tests | 7 archivos: unitarios, dos de integración con Postgres real y el flujo entero |

⚠️ Los documentos hablan de 93 tablas y 73 permisos: eso es **el sistema completo**, no lo
construido. Lo construido es el hito 1. Cuando un documento y una migración se contradigan,
manda la migración.

---

## Cómo es cada lección

Siempre la misma forma, para que sepas qué esperar:

1. **Qué problema resuelve** — en una frase, sin palabras técnicas.
2. **El recorrido** — leemos el código real, archivo por archivo, con enlaces a la línea.
3. **Una pregunta** — la respondes tú. Si no sale, volvemos atrás; no seguimos.
4. **Qué romper** — un cambio pequeño que hace fallar algo, para que veas quién sostiene qué.

Las lecciones 1 a 11 son la **pista A** (selección de personal: tu código). La 12 es la
**pista B** (motor de agentes: código de Ricardo, solo lectura). Van al final porque no
comparten nada con la pista A y mezclarlas confunde las dos.

---

## Pista A · La selección de personal

### 1. El mapa: dos proyectos en un repositorio

Por qué hay 208 clases que no se hablan entre sí. Los 15 paquetes de dominio, qué guarda cada
uno, y la frontera: **una sola clase** cruza de un lado al otro
(`ai/exception/ResourceNotFoundException`, usada en cinco servicios). Las dos clases que
enumeran controladores a mano y hay que tocar al añadir uno nuevo: `ManejadorErrores` y
`ConfiguracionSwagger`.

Sales sabiendo: dónde vive cada cosa y por qué el código está partido así.

### 2. El arranque: qué pasa antes de la primera petición

`AiEngineApplication`, `application.yaml`, `docker-compose.yml` (Postgres en 5433, RabbitMQ).
Qué es un bean y quién los crea. Por qué la aplicación **no arranca sin `DEEPSEEK_API_KEY`**
aunque la selección de personal no use IA. Dónde viven los secretos y por qué fuera del
repositorio.

### 3. Los datos primero: Flyway manda

Antes que cualquier clase, el esquema. Las nueve migraciones en orden, qué crea cada una, y la
regla dura: **una migración aplicada no se edita jamás**, se escribe otra. `ddl-auto: validate`:
JPA no crea ni cambia tablas, solo comprueba que las entidades cuadren con lo que hay. Las
semillas de V9: estados, roles, permisos, pesos, parámetros y textos.

Qué romper: cambia un campo de una entidad sin migración y mira el error de arranque.

### 4. Una petición de punta a punta

**La lección central.** Tomamos el endpoint más simple del panel y lo seguimos entero:

```
petición → filtro → controlador → DTO de entrada → servicio (interfaz)
        → implementación → repositorio → entidad → base de datos
        → mapper → DTO de salida → respuesta
```

Qué hace cada capa, por qué existe, y qué pasaría si la quitas. Todo lo demás en este backend
es una variación de este recorrido: cuando lo entiendas, sabes leer los otros 100 archivos.

### 5. Seguridad · las dos puertas

El dominio más grande de selección (16 clases). Dos formas de entrar al sistema —el candidato
con cuenta propia, el equipo con el token que emite RENASER OS— y por qué no se parecen.
`FiltroIdentidad`, `ServicioToken`, `ServicioContexto` y quién es el usuario durante una
petición.

### 6. Seguridad · permisos con alcance

Un rol no está en el código: es un conjunto de permisos en la base de datos. `Permisos.alcanceDe`
y la parte que casi todo el mundo entiende mal: **el alcance se aplica dentro de la consulta**, no
filtrando después en Java. Ver `TODO` frente a `PROPIO` en las 65 asignaciones y en las queries.

Qué romper: quita el alcance de una consulta y mira cómo un candidato ve postulaciones ajenas.

### 7. La máquina de estados

Los 18 estados como rejilla: cinco etapas por cuatro momentos, más los tres finales. Por eso el
siguiente estado se **calcula** en vez de estar escrito uno por uno. `MaquinaEstados.transicionar`
como único camino posible, y el historial que no se borra.

Leemos la clase junto a `MaquinaEstadosTest`: ese test es la especificación ejecutable de las
reglas, y se entiende mejor que cualquier explicación.

### 8. Los dominios, en el orden del negocio

Ahora sí, uno por uno, siguiendo el recorrido real de una contratación:

`solicitud` (qué falta en la empresa) → `vacante` + `pesos` (qué se busca y cuánto vale cada
etapa) → `postulacion` (la persona avanzando) → `portal` (todo lo que ve el candidato en un
controlador y un servicio).

### 9. Lo transversal

`archivo` (almacén en disco, enlaces firmados de corta duración), `notificacion` (el correo se
**registra pero no se envía**: falta el dominio de Renaser), `auditoria` (registro que no se
modifica), `parametro` (lo que Renaser cambia sin programar), `consentimiento`, `administracion`.

### 10. Errores y contratos

`ManejadorErrores` y por qué enumera controladores a mano. Coexisten **dos rutas de error** en
el mismo proceso: la tuya y el `GlobalControllerAdvice` del motor de agentes. Qué respuesta ve
el frontend en cada caso. Swagger y el candado.

### 11. Los tests

`MigracionesIT` levanta un Postgres real con Testcontainers y aplica las nueve migraciones.
`FlujoHito1IT` recorre el hito 1 entero de una pieza: es la forma más rápida de ver el sistema
funcionando sin abrir el navegador. Cómo se corren y cómo leer un fallo.

---

## Pista B · El motor de agentes

### 12. `ai/` de un vistazo

Solo lectura: no se toca, pero conviene entenderlo porque comparte proceso, base de datos y
puerto. Los 15 agentes y su respuesta estructurada, `FlowController`, el encadenamiento por
RabbitMQ con `routing[]` y tope de profundidad, los prompts en `resources/prompts/`, y el RAG
con pgvector más embeddings de Ollama.

---

## Cierre · qué falta en el sistema

No es relleno: saber qué **no** existe es parte de entenderlo.

| Hueco | Estado |
|---|---|
| La IA no lee ni califica a ningún candidato | Es el hito 2 |
| La identidad del equipo es un `dev-login` | El contrato con RENASER OS no existe aún |
| El correo se registra pero no sale | Falta el dominio de Renaser |
| Hitos 2 y 3 | Definidos en [Alcance del MVP](08-ALCANCE-DEL-MVP.md), sin construir |

---

## Ritmo

Una lección por sesión. La 4 es la más importante y puede llevar dos. Las 5 y 6 van juntas o
ninguna se entiende. Nada de esto es lectura pasiva: en cada lección abres el archivo, y al
final rompes algo.
