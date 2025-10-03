package org.acme;

import java.util.List;
import java.util.Map;

public class MaasRequest {
    public List<Message> messages;
    public List<Tool> tools;  // NEW: Tool definitions
    public String tool_choice;  // NEW: "auto" or "required"

    public MaasRequest(List<Message> messages) {
        this.messages = messages;
    }
    
    public MaasRequest(List<Message> messages, List<Tool> tools) {
        this.messages = messages;
        this.tools = tools;
        this.tool_choice = "auto";  // Let LLM decide when to call
    }

    public static class Message {
        public String role;
        public String content;
        public List<ToolCall> tool_calls;  // NEW: For assistant tool calls
        public String tool_call_id;  // NEW: For tool responses

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public Message(String role, String content, String tool_call_id) {
            this.role = role;
            this.content = content;
            this.tool_call_id = tool_call_id;
        }
    }
    
    // NEW: Tool definition structure
    public static class Tool {
        public String type = "function";
        public Function function;
        
        public Tool(Function function) {
            this.function = function;
        }
    }
    
    public static class Function {
        public String name;
        public String description;
        public Parameters parameters;
        
        public Function(String name, String description, Parameters parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
    }
    
    public static class Parameters {
        public String type = "object";
        public Map<String, Property> properties;
        public List<String> required;
        
        public Parameters(Map<String, Property> properties, List<String> required) {
            this.properties = properties;
            this.required = required;
        }
    }
    
    public static class Property {
        public String type;
        public String description;
        public List<String> enumValues;  // For enum types
        
        public Property(String type, String description) {
            this.type = type;
            this.description = description;
        }
        
        public Property(String type, String description, List<String> enumValues) {
            this.type = type;
            this.description = description;
            this.enumValues = enumValues;
        }
    }
    
    public static class ToolCall {
        public String id;
        public String type;
        public FunctionCall function;
    }
    
    public static class FunctionCall {
        public String name;
        public String arguments;  // JSON string
    }
}