package org.acme;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MaasRequest {
    @JsonProperty("prompt")
    public String prompt;
    
    @JsonProperty("max_tokens")
    public Integer maxTokens;

    public MaasRequest() {
    }

    public MaasRequest(String prompt) {
        this.prompt = prompt;
        this.maxTokens = 200; // Allow complete responses
    }
}