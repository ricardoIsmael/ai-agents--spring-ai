# RENASER OS · AI Engine

Backend de orquestación de agentes IA para RENASER OS. Implementa los 15 agentes del contrato `RENASER_AGENT_CONSTITUTION_V2`: cada uno recibe un `objective`, razona con un modelo de lenguaje sobre datos reales (cuando existen) y devuelve un envelope estructurado (`severity`, `facts`, `missingData`, `confidence`, `humanGate`, `nextActions`, `routing`, `payload`).

Este servicio cubre la parte de razonamiento con IA. Los 8 motores determinísticos y las 16 tools/herramientas del contrato V2 son responsabilidad de otro servicio (fuera de este repo).

## Stack

- Java 25 · Spring Boot 4.1 · Spring AI 2.0.0
- PostgreSQL + pgvector (persistencia de corridas + vector store del RAG)
- RabbitMQ (ejecución async y fan-out de agentes vía `routing[]`)
- Ollama (modelos locales)
- Supabase (fuente de datos reales de producción para algunos agentes)

## Modelos usados

| Modelo | Uso | Notas |
|---|---|---|
| `gemma4:e4b` | Modelo por defecto — razonamiento de los 15 agentes | ~9.6GB. En CPU sin GPU, prompts largos (agentes con contexto real inyectado) pueden tardar 30s–2min. |
| `qwen3-embedding:0.6b` | Embeddings para el vector store (RAG) | Usado solo en la ingesta/búsqueda de documentos, no en el chat. |

`qwen3:0.6b` se probó como modelo liviano para acelerar `ORCHESTRATOR` y `NARRATIVE_MESSAGE`, pero se descartó en ambos casos: para `ORCHESTRATOR` la calidad de routing no aplicaba las reglas mínimas de negocio, y para `NARRATIVE_MESSAGE` con contexto real inyectado producía JSON truncado (su ventana de contexto de 4096 tokens no alcanza) y colgaba reintentando. Ver `AgentModelSelectorImpl` si se quiere retomar la idea con otro modelo intermedio.

## Requisitos previos

- Java 25 (`JAVA_HOME` apuntando a un JDK 25)
- Docker (para Postgres + RabbitMQ vía `docker-compose.yml`)
- [Ollama](https://ollama.com) corriendo localmente, con los modelos descargados:

```bash
ollama pull gemma4:e4b
ollama pull qwen3-embedding:0.6b
```

## Configuración

La mayoría de la config vive en `src/main/resources/application.yaml` con valores por defecto que calzan con `docker-compose.yml` (uso local, no son secretos de producción):

- Postgres: `localhost:5433`, db `renaser_db`, user `postgres`
- RabbitMQ: `localhost:5672`, user `guest`
- Ollama: `localhost:11434`

El único secreto real es la **service role key de Supabase** (agentes CONSULTING, COLLECTIONS, OPERATIONS, GROWTH, EVENT, AUDITOR, NARRATIVE_MESSAGE leen datos reales de ahí). No va commiteada. Para configurarla localmente:

```bash
cp application-secrets.yaml.example application-secrets.yaml
```

Y pega tu key ahí (Supabase Dashboard → Project Settings → API Keys → Legacy API Keys → `service_role`). Este archivo está en `.gitignore`.

Alternativa sin archivo: variable de entorno `SUPABASE_SERVICE_ROLE_KEY`.

## Levantar el proyecto

```bash
docker-compose up -d
ollama serve   # si no está corriendo ya como servicio
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`. Documentación interactiva (Swagger):

```
http://localhost:8080/swagger-ui/index.html
```

## Datos reales por agente

Algunos agentes leen datos reales desde Supabase (ver `AgentExecutionServiceImpl.buildUserMessage`). Los agentes company-wide (CONSULTING, OPERATIONS, GROWTH, AUDITOR, NARRATIVE_MESSAGE) ignoran `entityId`; COLLECTIONS y EVENT lo usan como filtro (nombre exacto de cliente/evento en la tabla correspondiente). CEO, CLIENT_SUCCESS, QA_GOVERNANCE y DIAGNOSTIC todavía no tienen tabla con datos reales que consultar. FINANCE y TALENT_INTELLIGENCE quedan fuera a propósito: esas partes de la app (pagos, postulaciones) siguen en desarrollo.
