package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MaasResponse {

    public List<Choice> choices;

    public static class Choice {
        public Message message;
        public String finishReason;
    }

    public static class Message {
        public String role;
        public String content;
    }
}