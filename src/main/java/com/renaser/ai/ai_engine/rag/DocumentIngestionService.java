package com.renaser.ai.ai_engine.rag;

import java.nio.file.Path;

public interface DocumentIngestionService {
    void ingestPdf(Path pdfPath);
}
