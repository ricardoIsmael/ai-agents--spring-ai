package com.renaser.ai.ai_engine.reclutamiento.seguridad.impl;

import com.renaser.ai.ai_engine.reclutamiento.identidad.Organizacion;
import com.renaser.ai.ai_engine.reclutamiento.identidad.OrganizacionRepository;
import com.renaser.ai.ai_engine.reclutamiento.identidad.Usuario;
import com.renaser.ai.ai_engine.reclutamiento.identidad.UsuarioRepository;
import com.renaser.ai.ai_engine.reclutamiento.seguridad.ProveedorIdentidadEquipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Implementación de DESARROLLO: encuentra al usuario del equipo por su id de RENASER OS
// y le emite un token propio, sin hablar con RENASER OS. Cuando exista el contrato real,
// la implementación nueva valida el token que RENASER OS emite y esta se retira.
@Service
@RequiredArgsConstructor
public class ProveedorIdentidadEquipoDesarrollo implements ProveedorIdentidadEquipo {

    private final UsuarioRepository usuarios;
    private final OrganizacionRepository organizaciones;

    @Override
    public Optional<Usuario> autenticarDesarrollo(String correo, String usuarioRenaserOsId) {
        // Sin contraseña: es un login de desarrollo, protegido por app.seguridad.dev-login-activo
        return organizaciones.findByCodigo("RENASER")
                .map(Organizacion::getId)
                .flatMap(orgId -> usuarios.findByOrganizacionIdAndUsuarioRenaserOsId(orgId, usuarioRenaserOsId))
                .filter(Usuario::isEsActivo);
    }
}
