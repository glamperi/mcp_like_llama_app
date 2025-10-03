# MCP-Like Llama Flight Compensation Chatbot

An intelligent flight compensation system that combines conversational AI (Llama 3.2 3B) with business rules engine (Drools) to automatically evaluate and approve flight compensation claims.

## Architecture Overview

This application demonstrates an MCP-like (Model Context Protocol) architecture where:
- **LLM (Llama 3.2 3B)** handles natural conversation and data collection
- **Drools Rules Engine** makes deterministic compensation decisions
- **Regex Extractors** parse user input to extract structured data
- **State Management** tracks conversation context across messages

### System Architecture Diagram

%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#fff','primaryTextColor':'#000','primaryBorderColor':'#666','lineColor':'#666','secondaryColor':'#fff','tertiaryColor':'#fff','background':'#fff','mainBkg':'#fff','secondBkg':'#fff','clusterBkg':'#f9f9f9','clusterBorder':'#999','edgeLabelBackground':'#fff'}}}%%
graph LR
    subgraph Frontend["🖥️ Frontend Apps"]
        WS_UI["WebSocket Chat UI<br/>index.html + script.js"]
        REST_UI["REST Chat UI<br/>index-rest.html"]
    end

    subgraph Backend["⚙️ Quarkus Backend"]
        WSR["WebSocketChatResource<br/>@WebSocket /websocket-chat"]
        RR["ChatRestResource<br/>@Path /chat"]
        
        CS["CompensationState<br/>flightNumber, issueType<br/>issueDuration, compensation<br/>loyaltyStatus, inClaimMode"]
        
        RE["Regex Extractors<br/>Flight: [A-Z]{2}\d{2,4}<br/>Duration: \d+ hours<br/>Amount: $\d+<br/>Loyalty: gold/silver/basic"]
        
        MC["MaasClient<br/>LLM API<br/>llama-3-2-3b"]
        
        FE["FlighCompensationEndPoint<br/>@Tool MCP-like"]
        
        KS["KieSession<br/>Drools Runtime"]
        
        RULES["rules.drl<br/>Delay/Cancellation/Luggage<br/>Loyalty bonuses<br/>$500 Hard cap"]
    end

    WS_UI -.WebSocket.-> WSR
    REST_UI -.HTTP POST.-> RR
    
    WSR --> CS
    RR --> CS
    
    WSR --> RE
    RR --> RE
    
    WSR -.LLM Chat.-> MC
    RR -.LLM Chat.-> MC
    
    WSR ==>|hasAllRequiredData| FE
    RR ==>|hasAllRequiredData| FE
    
    FE --> KS
    KS --> RULES
    RULES ==>|Approved $| KS
    KS ==> FE
    
    style WS_UI fill:#A5D6FF,stroke:#333,stroke-width:2px,color:#000
    style REST_UI fill:#A5D6FF,stroke:#333,stroke-width:2px,color:#000
    style WSR fill:#90EE90,stroke:#333,stroke-width:2px,color:#000
    style RR fill:#90EE90,stroke:#333,stroke-width:2px,color:#000
    style CS fill:#FFE680,stroke:#333,stroke-width:2px,color:#000
    style RE fill:#FFB3B3,stroke:#333,stroke-width:2px,color:#000
    style FE fill:#DDA0DD,stroke:#333,stroke-width:2px,color:#000
    style KS fill:#90EE90,stroke:#333,stroke-width:2px,color:#000
    style RULES fill:#98FB98,stroke:#333,stroke-width:2px,color:#000
    style MC fill:#FFB3B3,stroke:#333,stroke-width:2px,color:#000
    
    style Frontend fill:#f9f9f9,stroke:#666,stroke-width:2px,color:#000
    style Backend fill:#f9f9f9,stroke:#666,stroke-width:2px,color:#000

mcp_like_llama_app/
├── chatbot-ui/              # Frontend chat interface
│   ├── index.html           # WebSocket-based chat UI
│   ├── index-rest.html      # REST-based chat UI  
│   └── script.js            # WebSocket client logic
│
└── drools-quarkus-airline/  # Backend Quarkus application
    ├── src/main/java/org/acme/
    │   ├── WebSocketChatResource.java    # WebSocket endpoint
    │   ├── ChatRestResource.java         # REST endpoint
    │   ├── FlighCompensationEndPoint.java # Drools integration (@Tool)
    │   ├── FlightIssue.java              # Fact object
    │   └── MaasClient.java               # LLM API client
    └── src/main/resources/
        ├── org/acme/rules.drl            # Drools business rules
        └── META-INF/kmodule.xml          # Drools configuration
```

## Features

- **Natural Language Processing**: Conversational interface powered by Llama 3.2 3B
- **Intelligent Data Extraction**: Regex-based extraction of flight details from natural language
- **Business Rules Engine**: Drools evaluates claims based on:
  - Issue type (delay, cancellation, luggage issues)
  - Flight duration/delay length
  - Customer loyalty tier (Basic, Silver, Gold)
  - Requested compensation amount
- **Dual Interface**: WebSocket and REST endpoints
- **MCP-Like Architecture**: Uses `@Tool` annotation for potential MCP integration

## Supported Claim Types

1. **Flight Delays** (> 2 hours)
   - Basic: $50
   - Silver: $100
   - Gold: $150

2. **Cancellations** (capped at $200)
   - With Gold loyalty bonus: +$50

3. **Luggage Issues** (lost, damaged, missing)
   - Basic: $75
   - Silver: $125
   - Gold: $200

All compensations subject to $500 hard cap.

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- OpenShift CLI (`oc`)
- Access to Llama 3.2 3B API endpoint

### Build & Run Backend
[drools-quarkus-airline/README.md](drools-quarkus-airline/README.md)


### Build & Run Frontend
[chatbot-ui/README.md](chatbot-ui/README.md)


## Configuration

Update `application.properties` in `drools-quarkus-airline/src/main/resources/`:

```properties
# LLM API endpoint
quarkus.rest-client.maas-client.url=https://your-llama-endpoint.com
quarkus.rest-client.maas-client.scope=jakarta.inject.Singleton
```

## How It Works

1. **User initiates conversation** via WebSocket or REST
2. **LLM conducts natural dialogue** to collect 5 required data points:
   - Flight number
   - Issue type
   - Duration (for delays/cancellations)
   - Requested compensation amount
   - Loyalty tier
3. **Regex extractors parse** user responses in real-time
4. **When all data collected**, backend automatically invokes Drools
5. **Drools evaluates** claim against business rules
6. **System returns decision** with explanation

## MCP-Like Design

The `@Tool` annotation on `FlighCompensationEndPoint` makes it MCP-compatible, though the current implementation uses direct Java method calls rather than LLM-driven tool invocation. This design allows for future migration to full MCP protocol with tool-calling LLMs (GPT-4, Claude 3, etc.).

## Technology Stack

- **Backend**: Quarkus 3.20, Java 21
- **Rules Engine**: Drools 9.44
- **LLM**: Llama 3.2 3B (via MaaS API)
- **Frontend**: Vanilla JavaScript, HTML, CSS
- **Communication**: WebSocket + REST

## Development

See detailed build instructions in:
- [chatbot-ui/README.md](chatbot-ui/README.md)
- [drools-quarkus-airline/README.md](drools-quarkus-airline/README.md)

## License

[Add your license here]

## Authors

Gary Lamperillo
