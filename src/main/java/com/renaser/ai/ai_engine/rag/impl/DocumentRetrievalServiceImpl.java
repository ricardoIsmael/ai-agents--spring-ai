package com.renaser.ai.ai_engine.rag.impl;

import com.renaser.ai.ai_engine.rag.DocumentRetrievalService;
import com.renaser.ai.ai_engine.rag.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentRetrievalServiceImpl implements DocumentRetrievalService {

    private final VectorStore vectorStore;

    @Override
    public List<SearchResultResponse> search(String query) {
        return vectorStore.similaritySearch(query).stream()
                .map(doc -> new SearchResultResponse(doc.getId(), doc.getText(), doc.getMetadata()))
                .toList();
    }
}
