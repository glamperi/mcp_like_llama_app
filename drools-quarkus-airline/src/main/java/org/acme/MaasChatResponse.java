package org.acme;

import java.util.List;

public class MaasChatResponse {
    public List<Choice> choices;
    public Usage usage;

    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason;
    }

    public static class Message {
        public String role;
        public String content;
        public List<ToolCall> tool_calls;  // ADD THIS
    }
    
    // ADD THIS CLASS
    public static class ToolCall {
        public String id;
        public String type;
        public FunctionCall function;
    }
    
    // ADD THIS CLASS
    public static class FunctionCall {
        public String name;
        public String arguments;  // JSON string
    }

    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }
}