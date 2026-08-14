package com.renaser.ai.ai_engine.supabase.impl;

import com.renaser.ai.ai_engine.supabase.ActividadRecord;
import com.renaser.ai.ai_engine.supabase.AvisoRecord;
import com.renaser.ai.ai_engine.supabase.CobroRecord;
import com.renaser.ai.ai_engine.supabase.EntregableRecord;
import com.renaser.ai.ai_engine.supabase.EventoRecord;
import com.renaser.ai.ai_engine.supabase.MotorRecord;
import com.renaser.ai.ai_engine.supabase.ProspectoRecord;
import com.renaser.ai.ai_engine.supabase.SupabaseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupabaseDataServiceImpl implements SupabaseDataService {

    private final RestClient supabaseRestClient;

    @Override
    public List<MotorRecord> getMotores() {
        MotorRecord[] motores = supabaseRestClient.get()
                .uri("/motores?select=*&order=n")
                .retrieve()
                .body(MotorRecord[].class);
        return motores == null ? List.of() : List.of(motores);
    }

    @Override
    public List<CobroRecord> getCobrosByCliente(String cliente) {
        CobroRecord[] cobros = supabaseRestClient.get()
                .uri("/cobros?select=*&cliente=eq.{cliente}&order=vence.asc", cliente)
                .retrieve()
                .body(CobroRecord[].class);
        return cobros == null ? List.of() : List.of(cobros);
    }

    @Override
    public List<ActividadRecord> getActividadesBloqueadas() {
        ActividadRecord[] actividades = supabaseRestClient.get()
                .uri("/actividades?select=titulo,estado,prioridad,bloqueo,avance,limite&bloqueo=not.is.null&limit=15")
                .retrieve()
                .body(ActividadRecord[].class);
        return actividades == null ? List.of() : List.of(actividades);
    }

    @Override
    public List<ProspectoRecord> getProspectos() {
        ProspectoRecord[] prospectos = supabaseRestClient.get()
                .uri("/crm_prospectos?select=*&order=created_at.desc")
                .retrieve()
                .body(ProspectoRecord[].class);
        return prospectos == null ? List.of() : List.of(prospectos);
    }

    @Override
    public List<EventoRecord> getEventoByNombre(String nombre) {
        EventoRecord[] eventos = supabaseRestClient.get()
                .uri("/eventos?select=*&nombre=eq.{nombre}", nombre)
                .retrieve()
                .body(EventoRecord[].class);
        return eventos == null ? List.of() : List.of(eventos);
    }

    @Override
    public List<EntregableRecord> getEntregablesPendientes() {
        EntregableRecord[] entregables = supabaseRestClient.get()
                .uri("/entregables?select=nombre,tipo,estado,version,created_at&estado=eq.pendiente_revision&order=created_at.asc&limit=15")
                .retrieve()
                .body(EntregableRecord[].class);
        return entregables == null ? List.of() : List.of(entregables);
    }

    @Override
    public List<AvisoRecord> getAvisosActivos() {
        AvisoRecord[] avisos = supabaseRestClient.get()
                .uri("/avisos?select=titulo,detalle,sev,area,responsable,leida&leida=eq.false&sev=neq.ok&order=created_at.desc&limit=8")
                .retrieve()
                .body(AvisoRecord[].class);
        return avisos == null ? List.of() : List.of(avisos);
    }
}
