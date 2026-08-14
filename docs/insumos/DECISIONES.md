# Decisiones tomadas

Actualizado: 2026-08-13
Fuente: entrevista con el usuario (Luis Rodrigo), documentos de Renaser y audio del cliente 08/08.

Este archivo guarda lo que ya está decidido. Lo que falta está en `NOTAS-TEMPORALES.md`.

---

## Qué es el proyecto

Módulo de selección de personal para **Renaser Consulting**. Recluta solo para Renaser,
no para clientes de la consultora. Backend nuevo en Java Spring Boot, conectado al
frontend Next.js de RENASER OS que ya existe.

**El sistema tiene dos caras:**
1. **Portal público de empleo** — donde postulan los candidatos.
2. **Panel de gestión** — dentro de RENASER OS, donde trabaja el equipo.

---

## Arquitectura

| Tema | Decisión |
|---|---|
| Backend | Java 25 + Spring Boot 4.1 (nuevo, lo construimos nosotros) |
| Frontend | Next.js, el que ya existe. El usuario también lo desarrolla |
| Comunicación | API REST con JSON. Proyectos separados, despliegue independiente |
| Base de datos | PostgreSQL propio con pgvector. En contenedor para desarrollar |
| Trabajo en segundo plano | RabbitMQ. Calificar no bloquea al candidato |
| Modelo de IA | **Ollama**, en el propio servidor. Se diseña para poder cambiarlo igual |

⚠️ **Actualizado el 14/08.** El repositorio del backend ya está creado y estas decisiones
vienen de él, no de una discusión previa. Antes se había documentado Supabase y «modelo de IA
sin decidir»; ambas cosas quedaron atrás.

⚠️ El archivo HTML `RENASER_OS_V5_1...html` que circula es un **mockup estático**: un solo
archivo, sin framework, sin llamadas a servidor. Los datos que muestra están escritos a mano.
No hay backend existente al cual conectarse.

---

## El embudo

```
1. Postula en el portal (cuenta propia, sube CV)
      |
      v  la IA puntúa el CV -> AQUI SE DESCARTA
2. Evaluación Integral (una sola experiencia para el candidato)
      - psicometría 30%  (pendiente de comprar)
      - banco por nivel 50%  (90 / 60 / 50 preguntas)
      - alineación personal 20%  (36 preguntas)
      |
      v
3. Prueba del puesto (cronometrada, con cambio inesperado a mitad)
      |
      v  la IA califica -> AQUI SE DESCARTA
4. Simulación de 2 h
      - sesión grupal en sala, cada uno en su pantalla
      - termina con conversación humana de 15-20 min
      |
      v
5. Validación práctica de 7 días
      |
      v
   Decisión: VERDE / ÁMBAR / ROJO
      |
      v
6. Seguimiento a 30 / 90 / 180 días  (comparar lo que predijo vs lo que pasó)
```

### Pesos de la nota final

| Componente | Dirección | Supervisión | Ejecución |
|---|---|---|---|
| CV y evidencia | 5% | 5% | 5% |
| Evaluación Integral | 20% | 20% | 15% |
| Prueba del puesto | 30% | 30% | 40% |
| Simulación 2 h | 25% | 25% | 20% |
| Validación 7 días | 20% | 20% | 20% |

---

## Cómo decide el sistema

| Tema | Decisión |
|---|---|
| Descarte automático | La IA descarta sola a quien está claramente por debajo. Los casos cerca del límite esperan revisión humana |
| Criterio para avanzar | Nota mínima configurable, con lista ordenada de mayor a menor |
| Preguntas de opción múltiple | Se corrigen solas contra la clave |
| Preguntas de texto libre | Las califica la IA con la guía de 0 a 4, y guarda su explicación |
| Prueba del puesto | La califica la IA contra la rúbrica. Una persona puede ajustar la nota; queda registrado quién y por qué |
| Barreras críticas | La IA las detecta y explica. Una persona confirma antes de que bloqueen |
| Alineación personal (semáforo) | Un ROJO **no descarta**. Genera preguntas para la conversación final |
| Preguntas de estilo (D01–D15, S01–S10, O01–O10) | **No suman nota.** Solo dibujan el perfil de la persona |
| Contradicciones e impresión ideal | Se marcan y alimentan la conversación final. No descartan |

---

## Reglas del examen

| Tema | Decisión |
|---|---|
| Guardado | Cada respuesta se guarda al momento. Si se corta, retoma donde quedó |
| Volver atrás | No puede modificar lo ya respondido |
| Orden | Preguntas y opciones se muestran en orden aleatorio |
| Lo que ve el candidato | Nunca ve claves, dimensiones, puntajes por opción ni lógica interna |
| Repostular | Si es del mismo nivel, se reutilizan sus respuestas. Si cambia de nivel, rinde el banco que corresponde |
| Cambio inesperado | Lo suelta el sistema solo, al minuto configurado, con su propio cronómetro |

---

## Roles

Candidato · Reclutador/Talento · Jefe del área que contrata · Dirección.

Los nombres y permisos **deben ser configurables**: es probable que cambien o se agreguen
más, alineados con los roles de RENASER OS.

**Bar Raiser**: existe, pero **solo opina**. Deja su revisión registrada, no puede bloquear.

**Validación de 7 días**: las 9 métricas se cargan a mano. Quién puede hacerlo (jefe del
área o RR.HH.) debe ser configurable.

---

## Datos personales

| Tema | Decisión |
|---|---|
| Antes de que la IA lea el CV | Se ocultan foto, edad, sexo y estado civil. La IA solo ve experiencia y logros |
| El reclutador | Sí ve el CV completo cuando lo abre |
| Consentimiento | Se acepta al crear la cuenta. Se guarda fecha, hora, versión del texto e IP |
| Seguimiento post-contratación | Los datos de desempeño vienen de RENASER OS ⚠️ dependencia externa |

---

## Versionado

Cada candidato queda atado a la **versión de banco y de pesos** con que fue evaluado.
Si el cliente cambia los pesos, las notas anteriores **no se recalculan**. Así se puede
reproducir cualquier decisión pasada tal como se tomó.

---

## Configuración

| Qué | Decisión |
|---|---|
| Las 200 preguntas + las 36 de alineación | Viven en base de datos, con pantalla para editarlas |
| Las 12 pruebas de puesto | Se cargan como plantillas editables. El cliente puede crear pruebas nuevas desde cero |
| Notificaciones | Correo automático en cada paso. WhatsApp queda para después |

---

## Panel de gestión

**Pantalla de inicio del reclutador**: arriba lo que está frenado esperando a una persona
(casos en zona dudosa, barreras por confirmar, sesiones sin programar). Abajo, resumen de
vacantes activas con cuánta gente hay en cada etapa.

**Ficha del candidato**: resumen arriba (nota global, semáforo, fortalezas, riesgos,
alineación personal). Cada etapa se despliega para ver respuesta por respuesta y por qué la
IA puso cada nota.

**Métricas para Dirección** — las cuatro:
1. Embudo por vacante: cuántos postularon, cuántos llegaron a cada etapa, dónde se cae más.
2. Tiempo desde postular hasta contratar, y cuánto se pierde en cada etapa.
3. Predicción contra desempeño real: nota de ingreso vs desempeño a 30/90/180 días.
4. Cuánto trabaja la IA sola: % de decisiones sin intervención humana, contra las metas del
   documento (90-95% en etapa 1, 95-100% en etapa 2).

---

## Base de datos y archivos

**Todo en servidores propios de Renaser**: la base PostgreSQL, el almacén de archivos y el
modelo de inteligencia artificial.

La base guarda solo la ruta del archivo, nunca el archivo. Así los entregables pesados
(videos, diseños) no inflan la base.

Cinco reglas que no se negocian:

1. **Spring Boot es el dueño de la identidad.** Los usuarios, contraseñas y permisos son
   tablas suyas. Tener permisos en dos capas distintas hace que ninguna sea confiable.

2. **La base nunca se expone a internet.** Solo la alcanza el backend, desde la red interna.
   Es la única copia de los datos personales de todos los candidatos.

3. **Las migraciones son de Flyway.** Toda la estructura vive en archivos versionados en el
   repositorio. Nadie crea ni modifica tablas a mano: el esquema dejaría de coincidir con lo
   que espera el backend.

4. **Almacén de archivos privado, siempre.** Un CV es dato personal. El acceso se da con
   enlaces firmados de corta duración que genera el backend cuando alguien abre el archivo.

5. **El modelo de IA corre en la misma máquina.** Los datos de los candidatos no se envían a
   ningún servicio de terceros.

✅ **Al no salir los datos del país, desaparece la parte más incómoda del consentimiento.**
Ya no hay que declarar envío al extranjero. Lo que sigue haciendo falta es fijar cuánto
tiempo se conservan los datos de quien no fue contratado; eso la ley 29733 sí lo exige.

⚠️ El costo se mueve de sitio, no desaparece: en vez de pagar por consulta hay que tener una
máquina capaz de sostener el modelo. Falta confirmar cuál.

**Uso de IA por el candidato**: el sistema **no** usa detectores. Le pregunta qué parte hizo
con IA y qué verificó él, y eso puntúa 5 de 100 en la prueba. Es lo que pide el documento:
se valora a quien usa IA y entiende lo que produjo.

---

## Plazos y abandonos

**No hay plazo para empezar.** El candidato responde cuando quiera; nadie se cae por tiempo.

**Pero al empezar la prueba, el cronómetro corre.** Cada prueba tiene su duración
(90–120 minutos según el puesto) y el cambio inesperado se dispara solo al minuto
configurado. Entrar es libre; una vez dentro, el reloj no se detiene.

Una postulación se cierra de tres formas:
1. **Automática** tras mucho tiempo sin avanzar (el periodo se configura).
2. **Manual** por el reclutador o el administrador.
3. **Automática** al cerrar la convocatoria.

Al cerrar una convocatoria, las postulaciones a mitad se cierran y se avisa a esas personas.
Sus datos quedan guardados, pero **no** se reutilizan automáticamente en otras vacantes.

El panel muestra cuántos días lleva cada candidato sin avanzar.

---

## Vacantes

El reclutador crea y publica directo, **sin aprobaciones**. Sale publicada en el portal.

---

## Comunicación de rechazo

Correo breve de agradecimiento, **sin explicar motivos, sin nota**. Se le indica que sus
datos quedan en la base para futuras convocatorias.

El rechazo se muestra en **tres lugares**: correo, portal del candidato y panel de gestión.

---

## Plazo del proyecto

Sin fecha comprometida. Se construye en orden lógico: vacantes y postulación primero,
luego evaluación, luego simulación y seguimiento.

---

## Pendientes de Renaser

1. Prueba psicométrica — hay que comprarla. Mientras no exista, su 30% se reparte entre
   los otros dos motores.
2. Retention Fit y Realistic Job Preview — el cliente los enviará. Se sabe que es una
   pantalla para registrar respuestas; las métricas no están definidas.
3. Qué máquina va a sostener el modelo de inteligencia artificial.
4. Cuánto tiempo se conservan los datos de un candidato no contratado.
5. Figura legal del periodo de validación de 7 días.
6. **Confirmar que el CV sí filtra** — contradice lo que el cliente dijo en el audio del 08/08.
