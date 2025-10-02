package com.example;

import java.util.List;

public class MaasRequest {
    public String model = "llama-3-2-3b";
    public List<Message> messages;

    public MaasRequest(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
