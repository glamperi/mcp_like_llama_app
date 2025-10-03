package org.acme;

import java.util.List;
import java.util.ArrayList;

public class MaasChatRequest {
    public List<MaasChatRequest.Message> messages;
    public List<MaasRequest.Tool> tools;
    public String tool_choice;

    public MaasChatRequest() {
    }

    public MaasChatRequest(List<MaasChatRequest.Message> messages) {
        this.messages = messages;
    }
    
    public MaasChatRequest(List<MaasRequest.Message> messages, List<MaasRequest.Tool> tools) {
        // Convert MaasRequest.Message to MaasChatRequest.Message
        this.messages = new ArrayList<>();
        for (MaasRequest.Message msg : messages) {
            Message chatMsg = new Message();
            chatMsg.role = msg.role;
            chatMsg.content = msg.content;
            chatMsg.tool_call_id = msg.tool_call_id;
            
            // Convert tool_calls if present
            if (msg.tool_calls != null && !msg.tool_calls.isEmpty()) {
                chatMsg.tool_calls = new ArrayList<>();
                for (MaasRequest.ToolCall tc : msg.tool_calls) {
                    MaasChatResponse.ToolCall responseToolCall = new MaasChatResponse.ToolCall();
                    responseToolCall.id = tc.id;
                    responseToolCall.type = tc.type;
                    responseToolCall.function = new MaasChatResponse.FunctionCall();
                    responseToolCall.function.name = tc.function.name;
                    responseToolCall.function.arguments = tc.function.arguments;
                    chatMsg.tool_calls.add(responseToolCall);
                }
            }
            
            this.messages.add(chatMsg);
        }
        this.tools = tools;
        this.tool_choice = "auto";
    }
    
    // Message class for MaasChatRequest
    public static class Message {
        public String role;
        public String content;
        public List<MaasChatResponse.ToolCall> tool_calls;  // Uses Response type for compatibility
        public String tool_call_id;
    }
}