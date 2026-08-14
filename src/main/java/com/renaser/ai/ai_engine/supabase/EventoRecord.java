package com.renaser.ai.ai_engine.supabase;

import java.math.BigDecimal;

public record EventoRecord(
        String id,
        String nombre,
        BigDecimal ingresos,
        BigDecimal egresos
) {}
