package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

import java.util.*;

@Path("/chat")
public class ChatRestResource {

    private static final Logger LOG = Logger.getLogger(ChatRestResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RestClient
    MaasClient maasClient;

    @Inject
    FlighCompensationEndPoint compensationEndpoint;
    
    @Inject
    ToolRegistry toolRegistry;  // NEW
    
    // Register tools on startup (if not already done by WebSocket resource)
    void onStart(@Observes StartupEvent ev) {
        if (!toolRegistry.hasTool("flightCompensation")) {
            toolRegistry.registerTool(compensationEndpoint);
            LOG.info("Tools registered from ChatRestResource: " + toolRegistry.getAllToolDefinitions().size());
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MaasChatResponse chat(ChatRequest request) {
        try {
            LOG.info("REST chat request: " + request.message);

            List<MaasRequest.Message> messages = new ArrayList<>();
            
            // System message
            messages.add(new MaasRequest.Message("system",
                "You are a helpful airline customer service agent with access to a flightCompensation tool. " +
                "When a customer mentions a flight issue, gather: flight number, issue type (delay/cancellation/luggage issues), " +
                "duration in hours, requested compensation amount in dollars, and loyalty status (basic/silver/gold). " +
                "IMPORTANT: Once you have ALL five pieces of information, you MUST immediately call the flightCompensation tool. " +
                "Do not ask for confirmation - just call the tool automatically with the gathered information."));
            // Add conversation history if provided
            if (request.history != null && !request.history.isEmpty()) {
                messages.addAll(request.history);
            }

            // Add current user message
            messages.add(new MaasRequest.Message("user", request.message));

            // Get tool definitions from registry - AUTO-GENERATED!
            List<MaasRequest.Tool> tools = toolRegistry.getAllToolDefinitions();

            // Call LLM with tool definitions
            MaasChatRequest maasRequest = new MaasChatRequest(messages, tools);
            MaasChatResponse response = maasClient.getChatCompletion(maasRequest).await().indefinitely();

            if (response.choices != null && !response.choices.isEmpty()) {
                MaasChatResponse.Choice choice = response.choices.get(0);

                // Check if LLM wants to call a tool
                if (choice.message.tool_calls != null && !choice.message.tool_calls.isEmpty()) {
                    LOG.info("LLM requested tool call");

                    // Convert MaasChatResponse.Message to MaasRequest.Message
                    MaasRequest.Message assistantMsg = new MaasRequest.Message(
                        choice.message.role, 
                        choice.message.content
                    );
                    assistantMsg.tool_calls = new java.util.ArrayList<>();
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
                    messages.add(assistantMsg);

                    // Process tool call using registry
                    MaasChatResponse.ToolCall toolCall = choice.message.tool_calls.get(0);
                    String toolResult = processToolCall(toolCall);

                    // Add tool result to messages
                    MaasRequest.Message toolMessage = new MaasRequest.Message("tool", toolResult, toolCall.id);
                    messages.add(toolMessage);

                    // Call LLM again to get final response with tool result
                    maasRequest = new MaasChatRequest(messages, tools);
                    response = maasClient.getChatCompletion(maasRequest).await().indefinitely();
                }
            }

            return response;

        } catch (Exception e) {
            LOG.error("REST chat error: " + e.getMessage(), e);
            throw new RuntimeException("Error processing chat: " + e.getMessage(), e);
        }
    }

    // SIMPLIFIED - now uses ToolRegistry
    private String processToolCall(MaasChatResponse.ToolCall toolCall) {
        try {
            LOG.info("Processing tool call: " + toolCall.function.name);
            LOG.info("Arguments: " + toolCall.function.arguments);

            // Parse JSON arguments
            Map<String, Object> args = objectMapper.readValue(toolCall.function.arguments, Map.class);

            // Invoke tool using registry (uses reflection)
            String result = toolRegistry.invokeTool(toolCall.function.name, args);

            LOG.info("Tool call result: " + result);
            return result;

        } catch (Exception e) {
            LOG.error("Error processing tool call: " + e.getMessage(), e);
            return "Error processing tool call: " + e.getMessage();
        }
    }

    public static class ChatRequest {
        public String message;
        public List<MaasRequest.Message> history;  // Optional conversation history
    }
}