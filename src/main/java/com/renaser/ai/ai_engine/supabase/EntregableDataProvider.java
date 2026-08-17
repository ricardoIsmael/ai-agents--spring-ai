package com.renaser.ai.ai_engine.supabase;

import java.util.List;

public interface EntregableDataProvider {

    // Entregables pendientes de revisión, los más antiguos primero (Auditor)
    List<EntregableRecord> getEntregablesPendientes();
}
