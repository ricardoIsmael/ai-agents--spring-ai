# Roles y permisos

Sistema de selección de personal — Renaser Consulting
Versión 1.1 · 2026-08-14

Este documento dice **quién puede hacer qué**, acción por acción.

## El contexto en cuatro líneas

Un candidato postula en el portal de Renaser y atraviesa cinco etapas: se lee su currículum,
responde una evaluación larga, hace una prueba del puesto cronometrada, asiste a una
simulación de dos horas y trabaja siete días. La inteligencia artificial califica las tres
primeras; las dos últimas siempre las evalúa una persona, igual que la decisión final.

El detalle está en [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md).

---

## Los cuatro roles

| Rol | Quién es | Dónde trabaja |
|---|---|---|
| **Candidato** | Alguien que postula | Portal público |
| **Reclutador** | Quien lleva el proceso día a día | Panel en RENASER OS |
| **Jefe del área** | El responsable del puesto que se busca | Panel en RENASER OS |
| **Dirección** | La máxima jerarquía | Panel en RENASER OS |

**Dirección ve y hace todo.** No hay nada en el sistema que le esté vedado: configura el
banco de preguntas, cambia los pesos, asigna roles, decide contrataciones y ve todas las
métricas. Es a la vez la autoridad de negocio y la administradora del sistema.

El **Bar Raiser** no es un rol: es una **función** que alguien asume para una vacante
concreta. Se explica más abajo.

---

## Regla base: los roles no están escritos en piedra

Los nombres y los permisos **se configuran desde el sistema**, no desde el código (ver «Administración» en los requisitos funcionales).
Esta tabla es cómo arranca el sistema, no cómo queda para siempre.

El cliente cambia cosas seguido, y probablemente estos roles terminen alineándose con los que
ya existen en RENASER OS. Por eso el sistema guarda **permisos**, no roles fijos: un rol es
simplemente un conjunto de permisos con nombre.

---

## Qué puede hacer cada uno

Leyenda: ● puede · ○ no puede · ◐ solo lo suyo

### Vacantes

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Ver vacantes publicadas | ● | ● | ● | ● |
| Crear una vacante | ○ | ● | ○ | ● |
| Editar una vacante | ○ | ● | ○ | ● |
| Publicarla (sin aprobación) | ○ | ● | ○ | ● |
| Cerrar una vacante | ○ | ● | ○ | ● |
| Elegir qué prueba se aplica | ○ | ● | ◐ | ● |
| Fijar la nota mínima | ○ | ● | ○ | ● |

El jefe del área puede opinar sobre la prueba de **sus** vacantes, pero quien la fija es el
reclutador.

### Candidatos

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Ver la lista de candidatos | ○ | ● | ◐ | ● |
| Abrir la ficha completa | ◐ | ● | ◐ | ● |
| Ver el CV sin ocultar datos | ◐ | ● | ◐ | ● |
| Ver respuesta por respuesta | ○ | ● | ◐ | ● |
| Ver por qué la IA puso cada nota | ○ | ● | ◐ | ● |
| **Ver las claves de puntuación** | ○ | ● | ○ | ● |
| Descargar entregables | ○ | ● | ◐ | ● |

**Las claves de puntuación las ven el Reclutador y Dirección.** Lo que dice el documento del
cliente es esto, textual:

> *"La clave de puntuación es INTERNA. **El candidato** nunca debe recibir este documento."*

La restricción es **contra el candidato**, no contra el equipo. Recursos Humanos es quien
diseña las evaluaciones, redacta preguntas nuevas y necesita entender por qué alguien sacó 2
en vez de 4. Sin acceso al banco no puede hacer su trabajo.

El **jefe del área no las ve**: no le hacen falta para decidir una contratación, y mientras
menos gente las conozca, menor el riesgo de que se filtren.

⚠️ **Lo que sí es absoluto**: las claves nunca viajan al portal del candidato, ni siquiera
escondidas en el código de la página (ver «Seguridad» en los no funcionales). Si viajan al navegador, cualquiera las lee y
el banco de 236 preguntas queda inutilizado.

### Evaluación y notas

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Responder su evaluación | ● | ○ | ○ | ○ |
| Revisar un caso en zona dudosa | ○ | ● | ○ | ● |
| Ajustar una nota de la IA | ○ | ● | ○ | ● |
| Confirmar un fallo grave | ○ | ● | ○ | ● |
| Ver las alertas de contradicción | ○ | ● | ◐ | ● |

Ajustar una nota y confirmar un fallo grave **siempre exigen motivo escrito**, y quedan
registrados con quién y cuándo (ver «Auditoría» en los no funcionales).

### Simulación

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Crear sesiones con fecha y cupo | ○ | ● | ○ | ● |
| Elegir su fecha | ● | ○ | ○ | ○ |
| Calificar la simulación | ○ | ● | ◐ | ● |
| Hacer la conversación final | ○ | ● | ◐ | ● |
| Marcar quién asistió | ○ | ● | ● | ● |
| Decidir qué hacer con un ausente | ○ | ● | ○ | ● |

### Validación de 7 días

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Iniciar el periodo | ○ | ● | ○ | ● |
| **Cargar las 9 métricas** | ○ | ● | ● | ● |
| Cerrar el periodo | ○ | ● | ● | ● |

Quién carga las métricas es **configurable**: puede ser solo el jefe del área, solo el
reclutador, o ambos. Arranca con ambos habilitados.

### La decisión final

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Ver el resultado del semáforo | ○ | ● | ◐ | ● |
| **Tomar la decisión final** | ○ | ○ | ● | ● |
| Pedir una prueba adicional (ámbar) | ○ | ● | ● | ● |
| Cambiar una decisión del sistema | ○ | ● | ● | ● |
| Opinar como Bar Raiser | ○ | ● | ● | ● |

⚠️ **La decisión de contratar es del jefe del área o de Dirección, no del reclutador.** El
reclutador lleva el proceso, prepara la evidencia y recomienda; pero quien se hace cargo del
resultado de esa persona es quien la va a tener en su equipo.

### Cierre de postulaciones

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Retirar su propia postulación | ● | ○ | ○ | ○ |
| Cerrar una postulación a mano | ○ | ● | ○ | ● |
| Reabrir una postulación cerrada | ○ | ● | ○ | ● |
| Pedir borrado de sus datos | ● | ○ | ○ | ○ |
| Ejecutar el borrado de datos | ○ | ○ | ○ | ● |

### Métricas

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Ver el embudo de sus vacantes | ○ | ● | ◐ | ● |
| Ver métricas de toda la empresa | ○ | ○ | ○ | ● |
| Ver predicción contra desempeño real | ○ | ○ | ○ | ● |
| Ver cuánto trabaja la IA sola | ○ | ○ | ○ | ● |
| Exportar datos | ○ | ● | ○ | ● |

### Configuración

| Acción | Candidato | Reclutador | Jefe área | Dirección |
|---|:--:|:--:|:--:|:--:|
| Editar el banco de preguntas | ○ | ● | ○ | ● |
| **Publicar una versión del banco** | ○ | ○ | ○ | ● |
| Crear o editar plantillas de prueba | ○ | ● | ○ | ● |
| Editar textos de correo | ○ | ● | ○ | ● |
| **Cambiar los pesos de las etapas** | ○ | ○ | ○ | ● |
| **Cambiar la zona dudosa** | ○ | ○ | ○ | ● |
| **Editar instrucciones de la IA** | ○ | ○ | ○ | ● |
| **Crear usuarios y asignar roles** | ○ | ○ | ○ | ● |
| **Crear roles nuevos** | ○ | ○ | ○ | ● |
| **Ver el registro de auditoría** | ○ | ○ | ○ | ● |

La división es simple: **el Reclutador prepara, Dirección aprueba.**

Recursos Humanos escribe preguntas, arma pruebas y ajusta los textos de correo — es su
trabajo diario. Pero **publicar una versión del banco** es de Dirección, porque a partir de
ese momento todos los candidatos se evalúan con ella.

Igual con los pesos, la zona dudosa y las instrucciones de la IA: definen **qué valora Renaser
al contratar**. Eso es decisión de negocio, no configuración operativa.

Trabajar en un borrador del banco sí puede hacerlo el Reclutador cuantas veces quiera. Lo que
no puede es ponerlo en producción solo.

---

## Qué significa "solo lo suyo" (◐)

**Candidato**: solo sus propias postulaciones, sus respuestas y su CV. Nunca ve a otros
candidatos ni sabe cuántos hay.

**Jefe del área**: solo los candidatos de **sus** vacantes. No ve las vacantes de otras áreas
ni sus candidatos.

Esto no es una decisión de diseño, es un requisito de seguridad (ver «Seguridad» en los no funcionales): el backend
lo verifica en cada llamada, no basta con esconder botones en la pantalla.

---

## El Bar Raiser

**No es un rol, es una función.** Se asigna por vacante: alguien que trabaja en Renaser pero
**no en el área que contrata**, para que no lo presione la urgencia de llenar el puesto.

| Qué hace | Qué NO hace |
|---|---|
| Revisa el expediente completo | **No puede bloquear** una contratación |
| Deja su opinión registrada | No decide |
| Señala qué falta validar | No aprueba por urgencia |

Su opinión queda visible junto a la decisión final, pero quien decide es el jefe del área o
Dirección.

**Regla al asignarlo:** el sistema no permite nombrar Bar Raiser al jefe del área que
contrata. Perdería todo el sentido.

---

## Una persona, varios roles

En una empresa del tamaño de Renaser es normal que alguien tenga más de un rol. Quien está en
Dirección puede ser también el jefe del área que contrata.

El sistema lo permite. Reglas:

1. Si una persona tiene dos roles, **puede hacer lo que le permita cualquiera** de los dos.
2. Pero **no puede ser Bar Raiser de una vacante donde es el jefe del área**.
3. Al registrar una acción se guarda **con qué rol la hizo**, para que la auditoría tenga
   sentido.

---

## Cómo se guarda esto

**Un rol es un conjunto de permisos con nombre.** No está escrito en el código.

```
PERMISO           un permiso suelto
                  ej: "puede_cerrar_vacante"

ROL               un nombre + una lista de permisos
                  ej: "Reclutador" = [crear_vacante,
                                      cerrar_vacante,
                                      revisar_dudoso, ...]

USUARIO           una persona + uno o varios roles
```

Así, cuando el cliente pida un cambio —y va a pedirlos— se marca o desmarca un permiso desde
una pantalla. No se toca código, no se despliega nada.

**El backend verifica el permiso en cada llamada** (ver «Seguridad» en los no funcionales). Ocultar un botón no es seguridad:
si el permiso no se valida en el servidor, cualquiera puede llamar a la API directamente.

---

## El panel de roles

Dirección administra los roles desde una pantalla: crea roles, les marca permisos y se los
asigna a las personas. Sin esa pantalla, cada cambio que pida el cliente obliga a programar.

Tres cosas pueden arruinar este panel. Las tres son fallos conocidos de este tipo de pantalla
y hay que evitarlas desde el diseño.

### 1 · Que Dirección se deje fuera del sistema

Alguien de Dirección desmarca por error el permiso de administrar roles, guarda, y ya nadie
puede volver a entrar a esa pantalla. **El sistema queda bloqueado** y solo se arregla
tocando la base de datos a mano.

**Cómo se evita:**
- Dirección **no puede editar su propio rol**. Si quiere cambiarlo, lo hace otra persona de
  Dirección.
- El sistema **impide guardar** si el cambio deja a cero las personas con permiso de
  administrar roles.
- El mensaje debe decir por qué: *"no puedes guardar: nadie quedaría con permiso para
  administrar roles"*.

### 2 · Que la pantalla se vuelva ilegible

Con unos 60 permisos y una casilla por cada uno, nadie entiende qué está marcando. La pantalla
se usa mal o se deja de usar.

**Cómo se evita:**
- Agrupar los permisos por área: vacantes, candidatos, evaluación, sesiones, decisión,
  configuración.
- Escribirlos en lenguaje normal —*"cerrar una vacante"*— y nunca con nombres técnicos como
  `vacancy.close`.
- Un interruptor por grupo para marcar o desmarcar todo el bloque.

```
  ROL · Reclutador                    18 de 60 permisos

  Vacantes                                    [4/6] v
     [x] Ver vacantes
     [x] Crear una vacante
     [x] Cerrar una vacante
     [ ] Fijar la nota mínima
     ...

  Configuración                               [2/9] >
  Candidatos                                  [7/12] >

                            [ Ver el sistema como este rol ]
```

**Vista previa.** El botón *"ver el sistema como este rol"* muestra qué pantallas y botones
tendría esa persona. Sin eso, Dirección marca casillas a ciegas y no sabe qué cambió hasta
que alguien se queja.

### 3 · Que un cambio rompa el trabajo de alguien que está en medio

A un reclutador le quitan el permiso de calificar mientras tiene una pantalla abierta. Cuando
guarda, le falla sin explicación y no entiende por qué.

**Cómo se evita:**
- Cuando el backend rechaza algo por permisos, el mensaje lo dice claro: *"ya no tienes
  permiso para calificar. Tus cambios no se guardaron"*.
- Nunca un error genérico ni una pantalla en blanco.
- El trabajo sin guardar **no se pierde de la pantalla**: la persona puede copiarlo antes de
  salir.

### Lo que nunca debe ser configurable

Hay cosas que no son permisos, son reglas del sistema. **Si aparecen como casillas, alguien
las va a marcar algún día.** Mejor que no existan como opción:

- Que un candidato vea a otros candidatos
- Que las claves de puntuación lleguen al portal del candidato
- Que se pueda borrar o modificar el registro de auditoría
- Que se pueda saltar el consentimiento de datos
- Que la inteligencia artificial decida una contratación sin una persona

### Todo cambio de permisos queda registrado

Quién lo hizo, cuándo, qué permiso, sobre qué rol y cuál era el valor anterior (ver «Auditoría» en los no funcionales). Es
el tipo de cambio que después nadie recuerda haber hecho.

---

## Lo que hay que confirmar con el cliente

⚠️ **Los documentos de Renaser no definen roles ni permisos.** Se revisaron los tres y no hay
ninguna tabla de accesos. Lo único que dicen sobre confidencialidad es que *el candidato*
nunca debe ver las claves.

Todo lo de este documento se dedujo del trabajo que hace cada uno. Conviene validar tres cosas:

| Qué decidí | Por qué | Si me equivoqué |
|---|---|---|
| El Reclutador **no** decide la contratación | Responde por esa persona quien la tendrá en su equipo | Se le marca ese permiso |
| El Jefe del área **no** ve las claves | No le hacen falta para decidir; menos gente que las conozca, menor riesgo | Se le marca ese permiso |
| Publicar el banco y cambiar pesos es de **Dirección** | Definen qué valora Renaser al contratar | Se le marcan al Reclutador |

En los tres casos el arreglo es marcar una casilla. Ninguno obliga a tocar código.

---

# Documentos relacionados

| Documento | Qué contiene |
|---|---|
| [Requisitos funcionales](01-REQUISITOS-FUNCIONALES.md) | Qué hace el sistema, etapa por etapa |
| [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) | Tecnología, seguridad, rendimiento |
| [Estados de la postulación](03-ESTADOS-POSTULACION.md) | Los 24 estados y cómo se pasa de uno a otro |
| [Embudo de selección](diagramas/embudo-seleccion.html) | Las cinco etapas, en un dibujo |
