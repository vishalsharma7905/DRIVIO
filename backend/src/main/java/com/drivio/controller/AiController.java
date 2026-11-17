package com.drivio.controller;

import com.drivio.dto.AiRequirementExtractionRequest;
import com.drivio.dto.AiRequirementExtractionResponse;
import com.drivio.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Assistance", description = "Llama 3.1 8B requirement extraction via Ollama with fallback")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/extract-requirements")
    @Operation(summary = "Parse natural language travel request into structured booking parameters")
    public ResponseEntity<AiRequirementExtractionResponse> extractRequirements(
            @Valid @RequestBody AiRequirementExtractionRequest request) {
        return ResponseEntity.ok(aiService.extractRequirements(request));
    }
}

