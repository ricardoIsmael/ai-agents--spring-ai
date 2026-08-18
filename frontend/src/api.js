// La única puerta al backend. Todo lo demás del frontend pide por aquí.
//
// El token vive en sessionStorage y no en localStorage a propósito: es una sesión del
// equipo mirando currículums de gente real, y no tiene por qué sobrevivir a cerrar la
// pestaña.

const BASE = '/api/v1'
const LLAVE = 'renaser.token'

export function token() {
  return sessionStorage.getItem(LLAVE)
}

export function cerrarSesion() {
  sessionStorage.removeItem(LLAVE)
}

async function pide(metodo, ruta, cuerpo) {
  const cabeceras = {}
  const t = token()
  if (t) cabeceras.Authorization = `Bearer ${t}`
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'

  const r = await fetch(`${BASE}${ruta}`, {
    method: metodo,
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })

  if (!r.ok) {
    // El backend responde con ProblemDetail: el detalle es lo único que sirve enseñar.
    let detalle = `${r.status}`
    try {
      const json = await r.json()
      detalle = json.detail || json.title || detalle
    } catch {
      // respuesta sin cuerpo: se queda el código
    }
    if (r.status === 401 || r.status === 403) cerrarSesion()
    throw new Error(detalle)
  }
  if (r.status === 204) return null
  const texto = await r.text()
  return texto ? JSON.parse(texto) : null
}

/** Entra como equipo. Es el atajo de desarrollo: RENASER OS todavía no emite el token. */
export async function entrar(uid) {
  const sesion = await pide('POST', '/panel/auth/dev-login', { usuarioRenaserOsId: uid })
  sessionStorage.setItem(LLAVE, sesion.token)
  return sesion
}

export const vacantes = () => pide('GET', '/panel/vacantes')
export const ranking = (vacanteId) => pide('GET', `/panel/vacantes/${vacanteId}/ranking`)
export const embudo = (vacanteId) => pide('GET', `/panel/vacantes/${vacanteId}/embudo`)
export const ficha = (postulacionId) => pide('GET', `/panel/postulaciones/${postulacionId}`)
export const perfil = (postulacionId) =>
  pide('GET', `/panel/postulaciones/${postulacionId}/perfil-integral`)
export const cribar = (postulacionId) =>
  pide('POST', `/panel/postulaciones/${postulacionId}/criba-cv`)
export const cribaRapida = (vacanteId) =>
  pide('POST', `/panel/vacantes/${vacanteId}/criba-rapida`)
export const cribaFina = (vacanteId) =>
  pide('POST', `/panel/vacantes/${vacanteId}/criba-fina`)
