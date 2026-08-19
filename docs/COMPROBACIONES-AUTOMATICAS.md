# Lo que el proyecto se comprueba solo

Qué mira la compilación por su cuenta, qué deja pasar, y qué falta por poner.

Sirve para saber de qué te avisa el proyecto y de qué no. Lo que no está aquí, no lo mira
nadie salvo una persona leyendo el código.

---

## Cómo se lanza todo

```bash
DOCKER_HOST='npipe:////./pipe/docker_engine' ./mvnw test -Dtest='!CalificacionIaRealIT'
```

Las dos cosas raras de esa línea tienen motivo:

- **`DOCKER_HOST`** hace falta en esta máquina. El contexto activo de Docker es
  `desktop-linux`, cuyo canal no existe; el que funciona es el `default`. Sin esto, todas las
  pruebas de integración fallan con «Could not find a valid Docker environment», y **eso no
  es un fallo del código**.
- **`CalificacionIaRealIT` se excluye a mano.** Llama al proveedor de verdad y gasta dinero.
  No tiene ninguna bandera que lo apague, y el `pom.xml` mete los `*IT.java` en la misma
  tanda que el resto, así que corre solo si no se dice lo contrario.

> ⚠️ **Conviene ponerle una bandera** a esa prueba (`@EnabledIfEnvironmentVariable`) para que
> no se dispare sin querer en la máquina de alguien que no sepa esto.

---

## 128 pruebas

| Qué | Cuántas | Necesita |
|---|---:|---|
| Unitarias, con dobles | 100 | nada |
| Arquitectura | 7 | nada |
| Integración, de punta a punta | 21 | Docker |

Las de integración levantan un PostgreSQL y un RabbitMQ de verdad con Testcontainers, y
recorren el flujo entero. **El modelo siempre se simula**: ninguna prueba de la tanda normal
llama al proveedor.

---

## Las siete reglas de arquitectura

Están en `ArquitecturaTest` y no inventan nada: son las reglas que el `CLAUDE.md` ya tenía
escritas en prosa. **Una regla en prosa se rompe sin que nadie se entere** —alguien añade un
import, el código compila, las pruebas pasan y la frontera ya no existe— y eso es lo que
estas siete impiden.

| Regla | Por qué importa |
|---|---|
| La selección solo cruza la frontera del motor de agentes por las clases acordadas | Son dos mitades que mantienen dos personas. La lista está enumerada: añadir una décima clase falla hasta que alguien la escriba ahí, y escribirla obliga a mirar si la frontera sigue teniendo sentido |
| Ningún controlador habla directamente con un repositorio | Entre la petición y la base hay permisos con alcance, transiciones y auditoría, y viven en el servicio |
| Ningún repositorio sabe de un servicio | Un círculo entre capas obliga a abrir media aplicación para leer una consulta |
| **Solo la máquina de estados cambia el estado de una postulación** | La más cara de romper. Saltársela no da error: la postulación se mueve igual. Lo que desaparece es el registro de quién la movió y por qué, el correo al candidato y la auditoría |
| Cada clase está en el paquete que su nombre promete | Quien busca un endpoint mira en `controller` |
| Las entidades no salen por un endpoint | Una entidad publicada convierte cualquier columna nueva en un cambio de contrato |
| Nadie escribe en la consola a pelo | Lo que se imprime así no aparece en el registro, y el registro es lo único que queda cuando algo falla en producción |

### Las dos desviaciones que ya existían

Al poner las reglas aparecieron dos sitios que se las saltan **desde antes**. Están
**nombrados uno por uno en la prueba, con su motivo**, y no escondidos tras un patrón
genérico: así la regla protege todo lo demás y la desviación sigue a la vista de quien
decida si vale la pena arreglarla.

| Dónde | Qué se salta | La defensa que tiene |
|---|---|---|
| `CatalogoController` | Inyecta cuatro repositorios y toca entidades | Devuelve catálogos de solo lectura —niveles, familias, etapas, estados— sin ninguna regla que aplicar. Un servicio ahí sería una capa que solo reenvía |
| `PanelAuthController` | Lo mismo | Monta el primer usuario del equipo cuando la base está vacía. Es el arranque, y todavía no hay servicio al que pedírselo |

**Es una decisión pendiente, no un olvido.** Si se prefiere que pasen por un servicio, se
quitan esos dos nombres de la prueba y ella misma dice qué falta.

---

## Lo que falta: la seguridad

**Hoy nadie mira el código buscando fallos de seguridad.** Ni la compilación, ni las pruebas,
ni las reglas de arquitectura. Las pruebas comprueban que el sistema hace lo que debe; no que
no haga lo que no debe.

Se va a cubrir con **Semgrep**, que lee el código buscando patrones peligrosos: consultas
armadas pegando cadenas, secretos escritos a mano, endpoints sin permiso, datos personales
que acaban en el registro.

**Cuándo toca usarlo:**

- Antes de una **auditoría de código** o de seguridad, propia o de un tercero.
- Cuando se toque algo que maneja **datos de candidatos**: currículums, correos, teléfonos.
  Este sistema mueve datos personales de gente real hacia dos proveedores externos.
- Al añadir un **endpoint nuevo**, para comprobar que lleva su permiso y su alcance.
- De forma periódica sobre la rama principal, para que una regresión no espere a la
  siguiente auditoría.

Queda pendiente decidir dos cosas: **qué reglas** se aplican —las que trae de serie para Java
y Spring, más las propias del proyecto— y **dónde corre**, si en la máquina de quien programa,
en la integración continua, o en las dos.

---

## Enlaces

- [Fallos corregidos de la criba](FALLOS-CORREGIDOS-CRIBA.md) — los cinco que aparecieron al
  pasar 190 currículums reales, y por qué cuatro no daban error
- [La criba de currículums](CRIBA-DE-CURRICULUMS.md) — el recorrido entero
- [Requisitos no funcionales](02-REQUISITOS-NO-FUNCIONALES.md) — lo que el sistema tiene que
  cumplir además de funcionar
