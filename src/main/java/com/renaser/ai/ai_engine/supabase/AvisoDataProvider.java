package com.renaser.ai.ai_engine.supabase;

import java.util.List;

public interface AvisoDataProvider {

    // Avisos activos no leídos con severidad relevante (Narrative Message)
    List<AvisoRecord> getAvisosActivos();
}
