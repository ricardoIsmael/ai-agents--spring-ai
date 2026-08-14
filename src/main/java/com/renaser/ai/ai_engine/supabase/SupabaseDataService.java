package com.renaser.ai.ai_engine.supabase;

import java.util.List;

public interface SupabaseDataService {

    // Prueba de humo: los 8 motores estratégicos ya sembrados en Supabase
    List<MotorRecord> getMotores();

    // Cobros de un cliente puntual, filtrados por nombre (entityId del agente Collections)
    List<CobroRecord> getCobrosByCliente(String cliente);

    // Actividades bloqueadas en toda la empresa (radar de cuellos de botella para Operations)
    List<ActividadRecord> getActividadesBloqueadas();

    // Embudo comercial completo (Growth)
    List<ProspectoRecord> getProspectos();

    // Un evento puntual, filtrado por nombre (entityId del agente Event)
    List<EventoRecord> getEventoByNombre(String nombre);

    // Entregables pendientes de revisión, los más antiguos primero (Auditor)
    List<EntregableRecord> getEntregablesPendientes();

    // Avisos activos no leídos con severidad relevante (Narrative Message)
    List<AvisoRecord> getAvisosActivos();
}
