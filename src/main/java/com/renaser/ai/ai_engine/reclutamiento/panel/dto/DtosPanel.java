package com.renaser.ai.ai_engine.reclutamiento.panel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Los contratos del panel del equipo.
public final class DtosPanel {

    private DtosPanel() {}

    // ---------- autenticación de desarrollo ----------

    public record DevLogin(@NotBlank String usuarioRenaserOsId) {}

    public record Sesion(String token, Long usuarioId) {}

    // ---------- Solicitud de Talento ----------

    public record CrearSolicitud(
            @NotNull Long areaId,
            @NotBlank String urgencia,
            String nivelPuestoCodigo,
            String familiaCodigo,
            @NotBlank String resultadoPrincipal,
            @NotBlank String motivo,
            @NotBlank String consecuenciaNoContratar,
            LocalDate requeridaPara,
            @NotBlank String analisisCapacidad,
            String capacidadesIndispensables,
            String capacidadesAprendibles,
            String modalidad,
            String horario,
            String compensacion,
            Long responsableUsuarioId,
            @NotNull @Size(min = 3, max = 5,
                    message = "Una solicitud lleva entre 3 y 5 resultados esperados")
            List<ResultadoEsperadoDto> resultadosEsperados) {}

    public record ResultadoEsperadoDto(@NotBlank String descripcion, String indicador) {}

    public record SolicitudResumen(Long id, String estado, String urgencia, Long areaId,
                                   String resultadoPrincipal, Instant creadoEn) {}

    public record SolicitudDetalle(SolicitudResumen resumen, String motivo,
                                   String consecuenciaNoContratar, String analisisCapacidad,
                                   String capacidadesIndispensables, String capacidadesAprendibles,
                                   LocalDate requeridaPara, String nivelPuestoCodigo, String familiaCodigo,
                                   List<ResultadoEsperadoDto> resultadosEsperados) {}

    public record Decidir(@NotBlank String motivo) {}

    // ---------- Vacantes ----------

    public record GuardarVacante(
            @NotNull Long solicitudTalentoId,
            @NotNull Long puestoId,
            @NotBlank String titulo,
            @NotBlank String descripcion,
            String proposito,
            String responsabilidades,
            String requisitos,
            String modalidad,
            String horario,
            String ubicacion,
            String compensacionPublica,
            @NotBlank String tipoCierre,
            Integer plazas,
            Instant abreEn,
            Instant cierraEn,
            @NotNull Long responsableUsuarioId) {}

    public record VacantePanel(Long id, String titulo, String estado, String tipoCierre,
                               Long puestoId, Long solicitudTalentoId, Long responsableUsuarioId,
                               Instant publicadaEn, Instant cerradaEn) {}

    public record GuardarRequisito(@NotBlank String descripcion, @NotBlank String regla) {}

    public record RequisitoPanel(Long id, String descripcion, String regla, boolean esActivo) {}

    public record GuardarPuesto(@NotBlank String codigo, @NotBlank String nombre,
                                @NotBlank String nivelPuestoCodigo, @NotBlank String familiaCodigo) {}

    // ---------- Postulaciones ----------

    public record FilaBandeja(Long postulacionId, String uuid, String candidato, String vacante,
                              String estado, String estadoNombre, String esperaA,
                              String grupoPrioridad, long diasSinCambio) {}

    public record FichaPostulacion(Long id, String uuid, String candidato, String correo,
                                   String vacante, String estado, String estadoNombre,
                                   String grupoPrioridad, String motivoCierre,
                                   String resultadoOrgulloso, List<String> enlaces,
                                   Long archivoCvId, Instant creadoEn, Instant movidoEn) {}

    public record PasoHistorial(String estadoAnterior, String estadoNuevo, Long usuarioId,
                                boolean fueElSistema, boolean fuePorLote, String motivo,
                                Instant ocurridaEn) {}

    public record Transicionar(@NotBlank String estadoDestino,
                               @NotBlank String motivo,
                               String motivoCierre) {}

    public record ConfirmarAvance(@NotBlank String motivo) {}

    public record ConteoEmbudo(Map<String, Long> porEstado) {}

    // ---------- Configuración y administración ----------

    public record EditarParametro(@NotBlank String valor, @NotBlank String motivo) {}

    public record ParametroPanel(String codigo, String valor, String tipo, String descripcion) {}

    public record NuevaPlantilla(@NotBlank String codigo, @NotBlank String asunto,
                                 @NotBlank String cuerpo) {}

    public record PlantillaPanel(Long id, String codigo, Integer version, String asunto,
                                 String cuerpo, boolean esActiva) {}

    public record FilaAuditoria(Long id, String accion, String entidad, Long entidadId,
                                Long usuarioId, String motivo, Instant ocurridaEn) {}

    public record SolicitudBorradoPanel(Long id, Long personaId, String motivo,
                                        Instant solicitadoEn, Instant ejecutadoEn) {}

    public record CrearUsuarioEquipo(@NotBlank String nombre, @NotBlank String apellidos,
                                     @NotBlank String correo, @NotBlank String usuarioRenaserOsId,
                                     Long areaId, @NotNull List<String> roles) {}

    public record UsuarioPanel(Long id, String correo, String usuarioRenaserOsId,
                               Long areaId, boolean esActivo, List<String> roles) {}

    public record AsignarRoles(@NotNull List<String> roles) {}

    public record RolPanel(Long id, String codigo, String nombre, boolean esSistema) {}
}
