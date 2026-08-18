import { useEffect, useState } from 'react'
import * as api from './api.js'

// El retrato de un candidato: por qué está donde está.
//
// La lista de criterios viene entera del backend, con huecos incluidos. Un criterio sin
// nota se pinta como hueco y no desaparece: que la IA no pudiera puntuar algo es
// información, y esconderlo haría creer que lo miró todo.

const NOMBRE_HALLAZGO = {
  FORTALEZA: 'Fortaleza',
  RIESGO_CRITICO: 'Riesgo crítico',
  RIESGO_DESARROLLABLE: 'Riesgo que se puede trabajar',
  PREFERENCIA: 'Preferencia',
  FALTA_EVIDENCIA: 'Falta evidencia',
}

const CLASE_HALLAZGO = {
  FORTALEZA: 'fortaleza',
  RIESGO_CRITICO: 'riesgo-critico',
  RIESGO_DESARROLLABLE: 'riesgo-desarrollable',
  PREFERENCIA: 'preferencia',
  FALTA_EVIDENCIA: 'falta-evidencia',
}

const ESPERA = {
  SIN_EMPEZAR: 'Nadie ha pedido todavía que la IA la lea.',
  EN_CURSO: 'La IA está leyendo el currículum. Tarda decenas de segundos.',
  FALLIDA: 'La IA falló y agotó sus reintentos. No se le inventó ninguna nota.',
}

function tiempo(meses) {
  if (meses == null) return null
  if (meses < 12) return `${meses} meses`
  const anios = Math.floor(meses / 12)
  const resto = meses % 12
  return resto === 0 ? `${anios} años` : `${anios} años ${resto} m`
}

function Medida({ etiqueta, valor }) {
  return (
    <div className="medida">
      <div className="cifra">{valor == null ? '—' : Number(valor).toFixed(0)}</div>
      <div className="etiqueta">{etiqueta}</div>
    </div>
  )
}

function Criterio({ nota }) {
  const puntaje = nota.puntaje == null ? null : Number(nota.puntaje)
  const maximo = Number(nota.maximo || 100)
  return (
    <div className={`criterio${puntaje == null ? ' hueco' : ''}`}>
      <div className="fila">
        <span className="nombre">{nota.criterio}</span>
        <span className="num apagado">
          {puntaje == null ? 'sin nota' : `${puntaje.toFixed(0)} / ${maximo.toFixed(0)}`}
        </span>
      </div>
      <div className="barra">
        <span style={{ width: `${puntaje == null ? 0 : (puntaje / maximo) * 100}%` }} />
      </div>
      <div className="porque">
        {nota.explicacion || 'La IA no pudo puntuar este criterio con lo que había en el currículum.'}
      </div>
    </div>
  )
}

export default function Ficha({ fila, onCambio }) {
  const [perfil, setPerfil] = useState(null)
  const [error, setError] = useState(null)
  const [pidiendo, setPidiendo] = useState(false)

  useEffect(() => {
    let vigente = true
    setPerfil(null)
    setError(null)
    api.perfil(fila.postulacionId)
      .then((p) => vigente && setPerfil(p))
      .catch((e) => vigente && setError(e.message))
    return () => { vigente = false }
  }, [fila.postulacionId])

  async function cribar() {
    setPidiendo(true)
    setError(null)
    try {
      await api.cribar(fila.postulacionId)
      onCambio()
    } catch (e) {
      setError(e.message)
    } finally {
      setPidiendo(false)
    }
  }

  const espera = ESPERA[fila.estadoCalificacion]

  return (
    <div className="ficha">
      <div className="titulo">
        <div>
          <h1>{fila.candidato}</h1>
          <div className="apagado pequeno">
            {fila.correo} · {fila.estadoNombre}
          </div>
        </div>
        <button onClick={cribar} disabled={pidiendo || fila.estadoCalificacion === 'EN_CURSO'}>
          {fila.estadoCalificacion === 'SIN_EMPEZAR' ? 'Que la IA lea el currículum'
            : 'Volver a leer el currículum'}
        </button>
      </div>

      {error && <div className="aviso error" style={{ marginTop: '1rem' }}>{error}</div>}
      {espera && <div className="aviso" style={{ marginTop: '1rem' }}>{espera}</div>}
      {fila.pasada === 'RAPIDA' && (
        <div className="aviso" style={{ marginTop: '1rem' }}>
          Estas notas son de la primera pasada, la rápida: sirven para ordenar la tanda, no
          para decidir. La segunda pasada las rehace con el modelo que razona.
        </div>
      )}

      {fila.datos && (
        <div className="datos">
          {[
            ['Correo', fila.datos.email],
            ['Teléfono', fila.datos.telefono],
            ['Último puesto', fila.datos.ultimoPuesto],
            ['Empresa', fila.datos.ultimaEmpresa],
            ['Experiencia', tiempo(fila.datos.experienciaMesesTotal)],
            ['Educación', fila.datos.educacionMaxima],
          ].filter(([, valor]) => valor).map(([etiqueta, valor]) => (
            <div key={etiqueta} className="dato">
              <span className="etiqueta">{etiqueta}</span>
              <span>{valor}</span>
            </div>
          ))}
        </div>
      )}

      {fila.datos?.habilidades && (
        <p className="pequeno" style={{ margin: '0.5rem 0 0' }}>
          <span className="apagado">Habilidades: </span>{fila.datos.habilidades}
        </p>
      )}

      {fila.datos?.perfilResumen && <p className="resumen">{fila.datos.perfilResumen}</p>}

      {perfil?.resumen && <p className="resumen">{perfil.resumen}</p>}

      <div className="medidas">
        <Medida etiqueta="Nota de la etapa" valor={fila.notaEtapa} />
        <Medida etiqueta="Currículum" valor={fila.notaCurriculum} />
        <Medida etiqueta="Adecuación" valor={perfil?.adecuacion} />
        <Medida etiqueta="Potencial" valor={perfil?.potencial} />
        <Medida etiqueta="Alto rendimiento" valor={perfil?.altoRendimiento} />
        <Medida etiqueta="Confianza de la evidencia" valor={perfil?.confianzaEvidencia} />
      </div>

      <div className="columnas">
        <div>
          <h3>Los ocho criterios del currículum</h3>
          {(perfil?.notasCriterio || []).map((n) => (
            <Criterio key={n.criterio} nota={n} />
          ))}
        </div>

        <div>
          <h3>Lo que la IA encontró</h3>
          {(perfil?.hallazgos || []).length === 0 && (
            <p className="apagado pequeno">Todavía no hay hallazgos.</p>
          )}
          {(perfil?.hallazgos || []).map((h, i) => (
            <div key={i} className={`hallazgo ${CLASE_HALLAZGO[h.tipo] || ''}`}>
              <span className="tipo">{NOMBRE_HALLAZGO[h.tipo] || h.tipo}</span>
              {h.descripcion}
              {h.evidencia && <div className="evidencia">«{h.evidencia}»</div>}
              {h.sugerencia && <div className="apagado">Qué hacer: {h.sugerencia}</div>}
            </div>
          ))}

          {(perfil?.alertas || []).length > 0 && (
            <>
              <h3>Avisos</h3>
              <p className="apagado pequeno">
                Un aviso no descarta a nadie: es una pregunta para la conversación final.
              </p>
              {perfil.alertas.map((a, i) => (
                <div key={i} className="hallazgo riesgo-desarrollable">
                  <span className="tipo">{a.tipo}</span>
                  {a.descripcion}
                </div>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
