package com.example;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;

@WebSocket(path = "/chat")
public class ChatResource {

    @RestClient
    MaasClient maasClient;

    @OnTextMessage
    public String onMessage(String userMessage) {
        // Create the message list, including the system message and user's prompt
        List<MaasRequest.Message> messages = new ArrayList<>();
        
        // This is your prompt engineering part. It sets the persona.
        messages.add(new MaasRequest.Message("system", "You are a helpful airline agent. Be brief and concise."));
        messages.add(new MaasRequest.Message("user", userMessage));

        // Create the request object
        MaasRequest request = new MaasRequest(messages);

        // Call the external LLM API and block until a response is received
        MaasResponse response = maasClient.getLlmResponse(request).await().indefinitely();

        // Extract and return the LLM's response
        return response.choices.get(0).message.content;
    }
}