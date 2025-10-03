# MCP-Like Llama Flight Compensation Chatbot

An intelligent flight compensation system that combines conversational AI (Llama 3.2 3B) with business rules engine (Drools) to automatically evaluate and approve flight compensation claims.

## Architecture Overview

This application demonstrates an MCP-like (Model Context Protocol) architecture where:
- **LLM (Llama 3.2 3B)** handles natural conversation and data collection
- **Drools Rules Engine** makes deterministic compensation decisions
- **Regex Extractors** parse user input to extract structured data
- **State Management** tracks conversation context across messages

### System Architecture Diagram

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#fff','primaryTextColor':'#000','primaryBorderColor':'#666','lineColor':'#666','secondaryColor':'#fff','tertiaryColor':'#fff','background':'#fff','mainBkg':'#fff','secondBkg':'#fff','clusterBkg':'#f9f9f9','clusterBorder':'#999','edgeLabelBackground':'#fff'}}}%%
graph TB
    subgraph "Frontend"
        UI[Web UI<br/>index.html]
        WS[WebSocket Client<br/>wss://.../*websocket-chat]
    end

    subgraph "OpenShift Route Layer"
        Route[OpenShift Route<br/>drools-quarkus-airline-mistral...]
    end

    subgraph "Backend Application Pod"
        subgraph "API Layer"
            WSR[WebSocketChatResource<br/>@WebSocket]
            REST[ChatRestResource<br/>@Path /chat]
        end

        subgraph "Tool Discovery & Registry"
            TR[ToolRegistry<br/>@ApplicationScoped]
            TR -->|Scans @Tool<br/>annotations| COMP
            TR -->|Auto-generates<br/>MaasRequest.Tool| TOOLDEF[Tool Definitions]
            TR -->|Invokes via<br/>reflection| COMP
        end

        subgraph "Business Logic"
            COMP[FlighCompensationEndPoint<br/>@Tool annotation<br/>@ToolArg parameters]
            DROOLS[Drools Rules Engine<br/>KieSession]
            COMP -->|Creates KieSession| DROOLS
            DROOLS -->|Evaluates| RULES[compensation.drl<br/>Business Rules]
        end

        subgraph "External LLM Integration"
            CLIENT[MaasClient<br/>@RestClient]
            REQ[MaasChatRequest<br/>messages + tools]
            RESP[MaasChatResponse<br/>tool_calls]
        end
    end

    subgraph "External Services"
        MISTRAL[Mistral AI API<br/>mistral-large-latest]
    end

    subgraph "Logging"
        LOGFILE[/deployments/<br/>flight-quarkus.log]
    end

    UI -->|User messages| WS
    WS -->|WebSocket<br/>connection| Route
    Route --> WSR
    UI -->|HTTP POST| Route
    Route --> REST

    WSR -->|Gets tools from| TR
    REST -->|Gets tools from| TR
    
    WSR -->|1. Send messages<br/>+ tool definitions| CLIENT
    REST -->|1. Send messages<br/>+ tool definitions| CLIENT
    
    CLIENT -->|HTTP POST<br/>with tools array| MISTRAL
    MISTRAL -->|Response with<br/>tool_calls| CLIENT
    
    CLIENT -->|2. Returns response| WSR
    CLIENT -->|2. Returns response| REST
    
    WSR -->|3. If tool_calls present<br/>invoke via registry| TR
    REST -->|3. If tool_calls present<br/>invoke via registry| TR
    
    TR -->|Reflection call| COMP
    COMP -->|Returns result| TR
    TR -->|Tool result| WSR
    TR -->|Tool result| REST
    
    WSR -->|4. Send tool result<br/>back to LLM| CLIENT
    REST -->|4. Send tool result<br/>back to LLM| CLIENT
    
    MISTRAL -->|Final response| CLIENT
    CLIENT -->|Final answer| WSR
    CLIENT -->|Final answer| REST
    
    WSR -->|Response| WS
    REST -->|JSON Response| UI
    WS -->|Display| UI

    WSR -.->|LOG.error()| LOGFILE
    REST -.->|LOG.error()| LOGFILE
    TR -.->|LOG.info()| LOGFILE
    COMP -.->|LOG.info()| LOGFILE

    style TR fill:#e1f5e1
    style TOOLDEF fill:#e1f5e1
    style COMP fill:#fff3cd
    style LOGFILE fill:#f8d7da
    style MISTRAL fill:#d1ecf1
```

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
