package com.renaser.ai.ai_engine.supabase;

import java.util.List;

public interface ActividadDataProvider {

    // Actividades bloqueadas en toda la empresa (radar de cuellos de botella para Operations)
    List<ActividadRecord> getActividadesBloqueadas();
}
