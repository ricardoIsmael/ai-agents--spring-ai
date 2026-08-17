package com.renaser.ai.ai_engine.rag;

// Ruta del PDF en el filesystem del servidor a ingerir en el vector store
public record IngestRequest(String path) {
}
