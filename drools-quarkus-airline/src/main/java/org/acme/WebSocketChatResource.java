package org.acme;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/websocket-chat")
public class WebSocketChatResource {

    private static final Logger LOG = Logger.getLogger(WebSocketChatResource.class);
    private static final Map<String, List<MaasRequest.Message>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 20;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RestClient
    MaasClient maasClient;

    @Inject
    WebSocketConnection connection;
    
    @Inject
    FlighCompensationEndPoint compensationEndpoint;
    
    @Inject
    ToolRegistry toolRegistry;
    
    // Register tools on startup
    void onStart(@Observes StartupEvent ev) {
        LOG.error("!!!!!! STARTUP METHOD EXECUTED !!!!!!");
        System.err.println("!!!!!! STDERR TEST !!!!!!");
        toolRegistry.registerTool(compensationEndpoint);
        LOG.info("Tools registered: " + toolRegistry.getAllToolDefinitions().size());
    }

    @OnOpen
    public void onOpen() {
        String connectionId = connection.id();
        List<MaasRequest.Message> history = new ArrayList<>();
        history.add(new MaasRequest.Message("system", 
            "You are a helpful airline customer service agent with access to a flightCompensation tool. " +
            "When a customer mentions a flight issue, gather: flight number, issue type (delay/cancellation/luggage issues), " +
            "duration in hours, requested compensation amount in dollars, and loyalty status (basic/silver/gold). " +
            "IMPORTANT: Once you have ALL five pieces of information, you MUST immediately call the flightCompensation tool. " +
            "Do not ask for confirmation - just call the tool automatically with the gathered information."));
        conversations.put(connectionId, history);
        LOG.error("========================================");
        LOG.error("NEW WEBSOCKET CONNECTION: " + connectionId);
        LOG.error("========================================");
        LOG.info("New WebSocket connection opened: " + connectionId);
    }

    @OnClose
    public void onClose() {
        String connectionId = connection.id();
        conversations.remove(connectionId);
        LOG.error("========================================");
        LOG.error("WEBSOCKET CONNECTION CLOSED: " + connectionId);
        LOG.error("========================================");
        LOG.info("WebSocket connection closed: " + connectionId);
    }

    @OnTextMessage
    public String onMessage(String message) {
        try {
            String connectionId = connection.id();
            
            LOG.error("========================================");
            LOG.error("WEBSOCKET MESSAGE RECEIVED: " + message);
            LOG.error("Connection ID: " + connectionId);
            LOG.error("========================================");
            
            List<MaasRequest.Message> history = conversations.get(connectionId);
            
            if (history == null) {
                history = new ArrayList<>();
                history.add(new MaasRequest.Message("system", 
                    "You are a helpful airline customer service agent with access to a flightCompensation tool. " +
                    "When a customer mentions a flight issue, gather: flight number, issue type (delay/cancellation/luggage issues), " +
                    "duration in hours, requested compensation amount in dollars, and loyalty status (basic/silver/gold). " +
                    "IMPORTANT: Once you have ALL five pieces of information, you MUST immediately call the flightCompensation tool. " +
                    "Do not ask for confirmation - just call the tool automatically with the gathered information."));
                conversations.put(connectionId, history);
            }

            LOG.info("WebSocket message from " + connectionId + ": " + message);

            // Manage history size
            if (history.size() > MAX_HISTORY_SIZE) {
                MaasRequest.Message systemMsg = history.get(0);
                history.clear();
                history.add(systemMsg);
            }

            // Add user message
            history.add(new MaasRequest.Message("user", message));

            // Get tool definitions from registry
            List<MaasRequest.Tool> tools = toolRegistry.getAllToolDefinitions();
            
            LOG.error("NUMBER OF TOOLS AVAILABLE: " + tools.size());
            if (tools.size() > 0) {
                LOG.error("Tool name: " + tools.get(0).function.name);
            }
            
            // Call LLM with tool definitions
            MaasChatRequest request = new MaasChatRequest(history, tools);
            
            LOG.error("CALLING MISTRAL API...");
            MaasChatResponse response = maasClient.getChatCompletion(request).await().indefinitely();
            LOG.error("MISTRAL API RESPONDED");

            if (response.choices != null && !response.choices.isEmpty()) {
                MaasChatResponse.Choice choice = response.choices.get(0);
                
                LOG.error("Response has tool_calls: " + (choice.message.tool_calls != null));
                if (choice.message.tool_calls != null) {
                    LOG.error("Number of tool_calls: " + choice.message.tool_calls.size());
                }
                
                // Check if LLM wants to call a tool
                if (choice.message.tool_calls != null && !choice.message.tool_calls.isEmpty()) {
                    LOG.error("!!!!! TOOL CALL DETECTED !!!!!");
                    LOG.info("LLM requested tool call");
                    
                    // Convert MaasChatResponse.Message to MaasRequest.Message
                    MaasRequest.Message assistantMsg = new MaasRequest.Message(
                        choice.message.role, 
                        choice.message.content
                    );
                    assistantMsg.tool_calls = new ArrayList<>();
                    if (choice.message.tool_calls != null) {
                        for (MaasChatResponse.ToolCall tc : choice.message.tool_calls) {
                            MaasRequest.ToolCall requestToolCall = new MaasRequest.ToolCall();
                            requestToolCall.id = tc.id;
                            requestToolCall.type = tc.type;
                            requestToolCall.function = new MaasRequest.FunctionCall();
                            requestToolCall.function.name = tc.function.name;
                            requestToolCall.function.arguments = tc.function.arguments;
                            assistantMsg.tool_calls.add(requestToolCall);
                        }
                    }
                    history.add(assistantMsg);
                    
                    // Process tool call using registry
                    MaasChatResponse.ToolCall toolCall = choice.message.tool_calls.get(0);
                    String toolResult = processToolCall(toolCall);
                    
                    // Add tool result to history
                    MaasRequest.Message toolMessage = new MaasRequest.Message("tool", toolResult, toolCall.id);
                    history.add(toolMessage);
                    
                    LOG.error("CALLING MISTRAL API AGAIN WITH TOOL RESULT...");
                    
                    // Call LLM again to get final response with tool result
                    request = new MaasChatRequest(history, tools);
                    response = maasClient.getChatCompletion(request).await().indefinitely();
                    
                    LOG.error("MISTRAL API RESPONDED WITH FINAL ANSWER");
                    
                    if (response.choices != null && !response.choices.isEmpty()) {
                        String finalResponse = response.choices.get(0).message.content;
                        history.add(new MaasRequest.Message("assistant", finalResponse));
                        LOG.error("FINAL RESPONSE: " + finalResponse);
                        LOG.error("========================================");
                        return finalResponse;
                    }
                } else {
                    LOG.error("NO TOOL CALL - NORMAL CONVERSATION");
                    // Normal conversation response
                    String botResponse = choice.message.content;
                    history.add(new MaasRequest.Message("assistant", botResponse));
                    LOG.error("BOT RESPONSE: " + botResponse);
                    LOG.error("========================================");
                    return botResponse;
                }
            }
            
            LOG.error("NO RESPONSE FROM MISTRAL");
            LOG.error("========================================");
            return "I'm sorry, I couldn't process your request at this time.";

        } catch (Exception e) {
            LOG.error("!!!!! ERROR IN WEBSOCKET !!!!!");
            LOG.error("Error: " + e.getMessage());
            e.printStackTrace();
            LOG.error("========================================");
            LOG.error("WebSocket error: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    private String processToolCall(MaasChatResponse.ToolCall toolCall) {
        try {
            LOG.error("========================================");
            LOG.error("PROCESSING TOOL CALL: " + toolCall.function.name);
            LOG.error("ARGUMENTS: " + toolCall.function.arguments);
            LOG.error("========================================");
            
            LOG.info("Processing tool call: " + toolCall.function.name);
            LOG.info("Arguments: " + toolCall.function.arguments);
            
            // Parse JSON arguments
            Map<String, Object> args = objectMapper.readValue(toolCall.function.arguments, Map.class);
            
            LOG.error("PARSED ARGUMENTS:");
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                LOG.error("  " + entry.getKey() + " = " + entry.getValue());
            }
            
            LOG.error("INVOKING TOOL VIA REGISTRY...");
            
            // Invoke tool using registry (uses reflection)
            String result = toolRegistry.invokeTool(toolCall.function.name, args);
            
            LOG.error("========================================");
            LOG.error("TOOL RESULT: " + result);
            LOG.error("========================================");
            
            LOG.info("Tool call result: " + result);
            return result;
            
        } catch (Exception e) {
            LOG.error("!!!!! ERROR PROCESSING TOOL CALL !!!!!");
            LOG.error("Error: " + e.getMessage());
            e.printStackTrace();
            LOG.error("========================================");
            LOG.error("Error processing tool call: " + e.getMessage(), e);
            return "Error processing tool call: " + e.getMessage();
        }
    }
}