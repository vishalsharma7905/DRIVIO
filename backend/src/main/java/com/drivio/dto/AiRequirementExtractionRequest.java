package com.drivio.dto;

import jakarta.validation.constraints.NotBlank;

public class AiRequirementExtractionRequest {

    @NotBlank(message = "Prompt text is required")
    private String prompt;

    public AiRequirementExtractionRequest() {}

    public AiRequirementExtractionRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}

