package com.renaser.ai.ai_engine.parametro.service;

import com.renaser.ai.ai_engine.parametro.repository.ParametroRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Lectura tipada de los parámetros que Renaser cambia sin programar. Si el parámetro
// no existe se usa el valor por defecto que pasa quien llama: el sistema no se cae
// porque alguien borró una fila de configuración.
@Service
@RequiredArgsConstructor
public class ServicioParametros {

    private final ParametroRepository parametros;

    public int entero(Long organizacionId, String codigo, int porDefecto) {
        return parametros.findByOrganizacionIdAndCodigo(organizacionId, codigo)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValor().trim());
                    } catch (NumberFormatException e) {
                        return porDefecto;
                    }
                })
                .orElse(porDefecto);
    }
}
