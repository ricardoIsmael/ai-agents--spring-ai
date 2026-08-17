#!/usr/bin/env python3
"""
Siembra datos de prueba en el sistema de selección, como si el portal ya existiera.

El portal del candidato todavía no está construido, así que sin esto la bandeja, la
ficha y el embudo nacen vacíos: justo las tres pantallas que más interesa probar.
Este script hace lo que hará el portal —crear cuentas, postular, subir el currículum—
llamando a los mismos endpoints públicos que usará él.

Todo pasa por la API, nunca por SQL. Así los datos salen con sus transiciones, sus
correos registrados y su auditoría, que es lo que hace que el panel se vea real.

Uso:
    python3 scripts/sembrar-datos-de-prueba.py [--api http://localhost:8080/api/v1] [--uid TU_UID]

Si se pasa --uid y la base está recién creada, ese id queda como primer usuario del
equipo con todos los roles, que es lo que necesita el panel de RENASER OS para entrar.
"""

import argparse
import io
import random
import sys
import time

import requests

# ---------------------------------------------------------------- catálogos

AREAS = ["Tecnología", "Operaciones", "Crecimiento", "Talento"]

# Los códigos salen de las semillas (V9). No se inventan: la base tiene claves
# foráneas contra `familia` y `nivel_puesto`, y un código que no existe revienta.
PUESTOS = [
    ("DEV_WEB", "Desarrollador web", "EJECUCION", "TECNOLOGIA"),
    ("LIDER_OPS", "Líder de operaciones", "SUPERVISION", "OPERACIONES"),
    ("CX_ANALISTA", "Analista de experiencia", "EJECUCION", "CRECIMIENTO"),
]

# (nombre, apellidos, cumple_requisitos)
CANDIDATOS = [
    ("Camila", "Torres Rivas", True),
    ("Diego", "Salazar Núñez", True),
    ("Fernanda", "Quispe Mamani", True),
    ("Joaquín", "Vargas Ureta", True),
    ("Valeria", "Ríos Castro", True),
    ("Mateo", "Ibáñez Flores", True),
    ("Lucía", "Chávez Paredes", True),
    ("Andrés", "Molina Guzmán", False),   # no confirma el requisito: se descarta solo
    ("Renata", "Espinoza León", True),
    ("Sebastián", "Cárdenas Rojo", True),
]

ORGULLOS = [
    "Rediseñé el flujo de citas de una clínica y el ausentismo bajó a la mitad en tres meses.",
    "Migré el sistema de facturación sin cortar el servicio ni un minuto.",
    "Levanté el tablero de indicadores que hoy usa Dirección todas las semanas.",
    "Automaticé el reporte mensual: pasó de dos días de trabajo a diez minutos.",
    "Acompañé a seis personas nuevas en su primer mes y las seis siguen en la empresa.",
]


def cv_falso(nombre):
    """Un PDF mínimo pero válido, para que la subida sea real."""
    texto = f"Curriculum de {nombre}"
    contenido = (
        b"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
        b"2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
        b"3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]>>endobj\n"
        b"trailer<</Root 1 0 R>>\n%% " + texto.encode("utf-8") + b"\n%%EOF\n"
    )
    return io.BytesIO(contenido)


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.token = None

    def _cab(self):
        return {"Authorization": f"Bearer {self.token}"} if self.token else {}

    def pide(self, metodo, ruta, **kw):
        r = requests.request(metodo, f"{self.base}{ruta}", headers=self._cab(), timeout=30, **kw)
        if not r.ok:
            detalle = ""
            try:
                detalle = r.json().get("detail", r.text[:200])
            except Exception:
                detalle = r.text[:200]
            raise RuntimeError(f"{metodo} {ruta} → {r.status_code}: {detalle}")
        if r.status_code == 204 or not r.content:
            return None
        try:
            return r.json()
        except Exception:
            return r

    get = lambda self, ruta: self.pide("GET", ruta)
    post = lambda self, ruta, datos=None: self.pide("POST", ruta, json=datos)


def paso(texto):
    print(f"  {texto}", flush=True)


def sembrar(api, uid_equipo):
    # ---------------------------------------------------------- 1. el equipo
    print("\n1 · El equipo entra")
    sesion = api.post("/panel/auth/dev-login", {"usuarioRenaserOsId": uid_equipo})
    api.token = sesion["token"]
    # La vacante exige un responsable: es quien acaba de entrar
    yo = sesion["usuarioId"]
    paso(f"{uid_equipo} entra con todos los roles (bootstrap de la base vacía)")

    # ---------------------------------------------------------- 2. estructura
    print("\n2 · La estructura de la empresa")
    areas = {}
    for nombre in AREAS:
        areas[nombre] = api.post("/panel/areas", {"nombre": nombre})["id"]
    paso(f"{len(areas)} áreas")

    puestos = {}
    for codigo, nombre, nivel, familia in PUESTOS:
        puestos[codigo] = api.post("/panel/puestos", {
            "codigo": codigo, "nombre": nombre,
            "nivelPuestoCodigo": nivel, "familiaCodigo": familia,
        })["id"]
    paso(f"{len(puestos)} puestos")

    # ------------------------------------------------- 3. solicitudes de talento
    print("\n3 · Las Solicitudes de Talento")
    guion = [
        ("Tecnología", "DEV_WEB", "aprobar", "NORMAL",
         "Sostener el desarrollo del portal de talento",
         "El equipo actual no llega a los plazos comprometidos",
         "Se retrasa el MVP y con él las inscripciones de setiembre",
         "Se evaluó automatizar parte del trabajo y no alcanza: lo que falta es diseño, no repetición."),
        ("Operaciones", "LIDER_OPS", "aprobar", "PRIORITARIA",
         "Cubrir los proyectos de obra que hoy no tienen responsable",
         "Dos proyectos entran en agosto y no hay quien los lleve",
         "Se posponen los dos proyectos y se pierde el trimestre",
         "Se revisó redistribuir entre los arquitectos actuales: ya están al 95% de su capacidad."),
        ("Crecimiento", "CX_ANALISTA", "aprobar", "NORMAL",
         "Medir de verdad la experiencia del cliente",
         "Hoy nadie mira las encuestas y se responden tarde",
         "Seguimos sin saber por qué se van los clientes",
         "Se evaluó darle esto a Operaciones y no hay horas: se probó dos meses y no se sostuvo."),
        ("Operaciones", "DEV_WEB", "rechazar", "NORMAL",
         "Un segundo analista de soporte",
         "El volumen de tickets subió en junio",
         "Los tiempos de respuesta se alargan",
         "Se evaluó y el pico fue estacional: en julio ya bajó."),
        ("Tecnología", "DEV_WEB", "pendiente", "URGENTE",
         "Alguien que sostenga la infraestructura",
         "Solo una persona conoce el despliegue",
         "Si esa persona falta, nadie puede desplegar",
         "No hay forma de automatizar esto sin alguien que lo diseñe primero."),
    ]

    solicitudes = []
    for area, puesto_cod, destino, urgencia, resultado, motivo, consecuencia, analisis in guion:
        s = api.post("/panel/solicitudes", {
            "areaId": areas[area], "urgencia": urgencia,
            "nivelPuestoCodigo": "EJECUCION", "familiaCodigo": "TECNOLOGIA",
            "resultadoPrincipal": resultado, "motivo": motivo,
            "consecuenciaNoContratar": consecuencia, "analisisCapacidad": analisis,
            "responsableUsuarioId": yo,
            "resultadosEsperados": [
                {"descripcion": "Entregar lo comprometido en el plazo", "indicador": "sin retrasos en el trimestre"},
                {"descripcion": "Dejar el trabajo documentado", "indicador": "documentación al día"},
                {"descripcion": "Que otra persona pueda continuarlo", "indicador": "al menos un relevo formado"},
            ],
        })
        if destino == "aprobar":
            api.post(f"/panel/solicitudes/{s['id']}/aprobacion", {"motivo": "Justificada: hay presupuesto y el análisis de capacidad es sólido"})
            solicitudes.append((s["id"], puesto_cod, resultado))
        elif destino == "rechazar":
            api.post(f"/panel/solicitudes/{s['id']}/rechazo", {"motivo": "El pico de tickets fue estacional; se revisa en octubre"})
    paso(f"{len(guion)} solicitudes: {len(solicitudes)} aprobadas, 1 rechazada, 1 esperando a Dirección")

    # ---------------------------------------------------------- 4. vacantes
    print("\n4 · Las vacantes")
    vacantes = []
    titulos = ["Desarrollador web", "Líder de operaciones", "Analista de experiencia del cliente"]
    for i, (solicitud_id, puesto_cod, _) in enumerate(solicitudes):
        v = api.post("/panel/vacantes", {
            "solicitudTalentoId": solicitud_id, "puestoId": puestos[puesto_cod],
            "titulo": titulos[i], "descripcion":
                "Trabajo con gente, no solo con herramientas. Buscamos a alguien que deje el trabajo "
                "mejor documentado de lo que lo encontró.",
            "tipoCierre": "PERMANENTE", "responsableUsuarioId": yo,
        })
        req = api.post(f"/panel/vacantes/{v['id']}/requisitos", {
            "descripcion": "Disponibilidad en Arequipa",
            "regla": "Reside en Arequipa o puede trasladarse antes de empezar",
        })
        api.post(f"/panel/vacantes/{v['id']}/publicacion")
        vacantes.append({"id": v["id"], "titulo": titulos[i], "requisito": req["id"]})
    paso(f"{len(vacantes)} vacantes publicadas, cada una con su requisito indispensable")

    # ------------------------------------------- 5. los candidatos (el portal)
    print("\n5 · Los candidatos, como si el portal existiera")
    token_equipo = api.token
    postulaciones = []

    for i, (nombre, apellidos, cumple) in enumerate(CANDIDATOS):
        correo = f"{nombre.lower()}.{apellidos.split()[0].lower()}@ejemplo.pe"
        vacante = vacantes[i % len(vacantes)]

        api.token = None  # el portal es público
        api.post("/portal/cuentas", {
            "nombre": nombre, "apellidos": apellidos, "correo": correo,
            "contrasena": "Demo12345!", "aceptaProceso": True,
            "aceptaFuturosContactos": i % 3 != 0,
        })
        api.token = api.post("/portal/auth/login", {"correo": correo, "contrasena": "Demo12345!"})["token"]

        campos = {
            "vacanteId": (None, str(vacante["id"])),
            "resultadoOrgulloso": (None, random.choice(ORGULLOS)),
            "portafolio": (None, f"https://{nombre.lower()}.dev"),
        }
        # Quien no confirma el requisito queda descartado en el acto: es el único
        # descarte automático del sistema, y conviene que se vea en los datos.
        if cumple:
            campos["requisitosConfirmados"] = (None, str(vacante["requisito"]))
        campos["cv"] = (f"cv-{nombre.lower()}.pdf", cv_falso(nombre), "application/pdf")

        r = requests.post(f"{api.base}/portal/postulaciones",
                          headers={"Authorization": f"Bearer {api.token}"},
                          files=campos, timeout=30)
        if not r.ok:
            raise RuntimeError(f"postular {nombre}: {r.status_code} {r.text[:200]}")
        postulaciones.append({"nombre": nombre, "uuid": r.json()["codigo"],
                              "cumple": cumple, "token": api.token, "vacante": vacante["titulo"]})
        paso(f"{nombre} postuló a «{vacante['titulo']}»" + ("" if cumple else "  → descartado: no cumple el requisito"))

    # ----------------------------------------------- 6. el equipo mueve la cola
    print("\n6 · El equipo mueve la cola")
    api.token = token_equipo
    bandeja = api.get("/panel/bandeja?espera_a=CANDIDATO")
    por_uuid = {f["uuid"]: f["postulacionId"] for f in bandeja}

    # A dónde llevar a cada uno, para que la bandeja tenga trabajo en los cuatro lados
    reparto = [
        ("PERFIL_POR_CONFIRMAR", "Perfil revisado a mano: la experiencia encaja con lo que pide el puesto"),
        ("PERFIL_POR_CONFIRMAR", "Perfil revisado a mano: buen recorrido, falta confirmar disponibilidad"),
        ("PRUEBA_TURNO_CANDIDATO", "Avanza a la prueba del puesto; se le envía el encargo por correo"),
        ("PRUEBA_POR_CONFIRMAR", "Prueba entregada y revisada por el equipo"),
        ("SIMULACION_POR_HABILITAR", "Pasa a simulación: falta abrir las fechas"),
        ("VALIDACION_POR_CONFIRMAR", "Terminó el periodo de validación; espera la lectura del área"),
        ("DECISION_POR_CONFIRMAR", "Todo el recorrido completo; queda la decisión del área"),
    ]

    movidos = 0
    for p, (destino, motivo) in zip([x for x in postulaciones if x["cumple"]], reparto):
        pid = por_uuid.get(p["uuid"])
        if not pid:
            continue
        api.post(f"/panel/postulaciones/{pid}/transiciones",
                 {"estadoDestino": destino, "motivo": motivo, "motivoCierre": None})
        movidos += 1
    paso(f"{movidos} postulaciones repartidas por las etapas")

    # Alguien se retira: es un final distinto del descarte, y el embudo lo distingue
    ultimo = [x for x in postulaciones if x["cumple"]][-1]
    api.token = ultimo["token"]
    api.post(f"/portal/postulaciones/{ultimo['uuid']}/retiro")
    paso(f"{ultimo['nombre']} se retiró del proceso por su cuenta")

    # Y alguien pide que le borren los datos, para que Administración tenga qué enseñar
    api.post("/portal/solicitudes-borrado", {"motivo": "Ya encontré trabajo, prefiero que borren mis datos"})
    paso(f"{ultimo['nombre']} pidió el borrado de sus datos")

    api.token = token_equipo
    return vacantes


def resumen(api):
    print("\n" + "=" * 58)
    print("  Lo que va a ver el panel")
    print("=" * 58)
    for etiqueta, ruta in [
        ("Solicitudes", "/panel/solicitudes"),
        ("Vacantes", "/panel/vacantes"),
        ("Áreas", "/panel/areas"),
    ]:
        print(f"  {etiqueta:<34} {len(api.get(ruta))}")
    for espera in ("TALENTO", "AREA", "CANDIDATO", "SISTEMA"):
        n = len(api.get(f"/panel/bandeja?espera_a={espera}"))
        print(f"  Bandeja · espera a {espera:<15} {n}")
    print(f"  Solicitudes de borrado             {len(api.get('/panel/solicitudes-borrado'))}")
    print("=" * 58)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--api", default="http://localhost:8080/api/v1")
    p.add_argument("--uid", default="dev-equipo",
                   help="El id de RENASER OS que quedará como primer usuario del equipo")
    args = p.parse_args()

    api = Api(args.api)
    try:
        api.get("/portal/vacantes")
    except Exception as e:
        print(f"No se pudo contactar con el backend en {args.api}\n  {e}", file=sys.stderr)
        print("\n¿Está levantado?  ./mvnw spring-boot:run", file=sys.stderr)
        return 1

    inicio = time.time()
    try:
        sembrar(api, args.uid)
    except RuntimeError as e:
        print(f"\nFalló la siembra: {e}", file=sys.stderr)
        return 1

    resumen(api)
    print(f"\nListo en {time.time() - inicio:.1f} s. Todas las contraseñas: Demo12345!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
