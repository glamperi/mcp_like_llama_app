package org.acme;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MaasChatRequest {
    @JsonProperty("messages")
    public List<Message> messages;
    
    @JsonProperty("max_tokens")
    public Integer maxTokens;

    public MaasChatRequest() {
    }

    public MaasChatRequest(List<Message> messages) {
        this.messages = messages;
        this.maxTokens = 200;
    }
    
    public static class Message {
        @JsonProperty("role")
        public String role;
        
        @JsonProperty("content")
        public String content;
        
        public Message() {
        }
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}