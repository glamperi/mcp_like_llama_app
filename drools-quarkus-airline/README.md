# Drools Quarkus Airline - Backend Service

Quarkus-based backend service that combines conversational AI (Llama 3.2 3B) with Drools business rules engine for automated flight compensation claim evaluation.

## Architecture

### Components

1. **Chat Endpoints**
   - `WebSocketChatResource.java` - WebSocket endpoint at `/websocket-chat`
   - `ChatRestResource.java` - REST endpoint at `/chat`

2. **State Management**
   - `CompensationState` - Tracks collected claim data per session
   - `ConcurrentHashMap` - Stores conversation history and state

3. **Data Extraction**
   - Regex patterns extract structured data from natural language
   - Handles flight numbers, issue types, durations, amounts, loyalty tiers

4. **Drools Integration**
   - `FlighCompensationEndPoint.java` - Bridge to Drools engine (annotated with `@Tool`)
   - `FlightIssue.java` - Fact object for Drools
   - `rules.drl` - Business rules for compensation approval

5. **External Services**
   - `MaasClient.java` - REST client for Llama 3.2 3B LLM API

## Prerequisites

- **Java**: 21 or higher
- **Maven**: 3.8+
- **OpenShift CLI**: `oc` command line tool
- **OpenShift Project**: Active project/namespace
- **Drools**: 9.44.0.Final (managed via dependencies)
- **LLM API Access**: Llama 3.2 3B endpoint with authentication

## Dependencies

Key dependencies in `pom.xml`:

```xml
<!-- Quarkus -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-websockets-next</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-openshift</artifactId>
</dependency>

<!-- Drools -->
<dependency>
    <groupId>org.drools</groupId>
    <artifactId>drools-drl-quarkus</artifactId>
    <version>9.44.0.Final</version>
</dependency>
<dependency>
    <groupId>org.drools</groupId>
    <artifactId>drools-mvel</artifactId>
    <version>9.44.0.Final</version>
</dependency>

<!-- MCP Server Support -->
<dependency>
    <groupId>io.quarkiverse.mcp</groupId>
    <artifactId>quarkus-mcp-server-sse</artifactId>
    <version>1.6.0</version>
</dependency>
```

## Configuration

Update `src/main/resources/application.properties`:

```properties
# HTTP Server
quarkus.http.port=8080

# LLM API Client
quarkus.rest-client.maas-client.url=https://llama-3-2-3b-maas-apicast-production.apps.prod.rhoai.rh-aiservices-bu.com
quarkus.rest-client.maas-client.scope=jakarta.inject.Singleton

# Add your API key/bearer token
# (Consider using environment variables in production)
```

## Build & Deploy to OpenShift

### Prerequisites
1. Login to OpenShift:
```bash
oc login --server=https://your-openshift-cluster:6443
```

2. Create or switch to your project:
```bash
oc new-project drools-airline
# or
oc project drools-airline
```

### Build and Deploy

From the `drools-quarkus-airline` directory, run:

```bash
./mvnw clean package -Dquarkus.openshift.deploy=true \
  -Dquarkus.kubernetes-client.trust-certs=true \
  -Dquarkus.openshift.route.expose=true
```

This command will:
- Clean and build the application
- Create a container image
- Deploy to your OpenShift project
- Create a Route (publicly accessible URL)
- Trust cluster certificates

### Verify Deployment

```bash
# Check pod status
oc get pods

# Check route
oc get route

# View logs
oc logs -f deployment/drools-quarkus-airline
```

### Access the Application

After deployment, get your route:
```bash
oc get route drools-quarkus-airline -o jsonpath='{.spec.host}'
```

Access the UI at:
- WebSocket UI: `https://<route-host>/index.html`
- REST UI: `https://<route-host>/rest.html`

## Local Development Mode

For local development with live reload:
```bash
./mvnw clean quarkus:dev
```

Access locally at:
- http://localhost:8080/index.html
- http://localhost:8080/rest.html

## Project Structure

```
src/main/
├── java/org/acme/
│   ├── WebSocketChatResource.java      # WebSocket endpoint
│   ├── ChatRestResource.java           # REST endpoint
│   ├── FlighCompensationEndPoint.java  # Drools integration (@Tool)
│   ├── FlightIssue.java                # Drools fact object
│   ├── MaasClient.java                 # LLM REST client interface
│   ├── MaasChatRequest.java            # LLM request DTO
│   └── MaasChatResponse.java           # LLM response DTO
│
└── resources/
    ├── application.properties          # Quarkus configuration
    ├── META-INF/
    │   ├── resources/                  # Static files (HTML, JS)
    │   │   ├── index.html
    │   │   ├── rest.html
    │   │   └── script.js
    │   └── kmodule.xml                 # Drools configuration
    └── org/acme/
        └── rules.drl                   # Business rules
```

## Drools Rules Overview

Located in `src/main/resources/org/acme/rules.drl`:

### Delay Compensation Rules
- **Basic tier**: $50 (delays > 2 hours)
- **Silver tier**: $100 (delays > 2 hours)
- **Gold tier**: $150 (delays > 2 hours)
- **Minor delays**: $0 (delays ≤ 2 hours)

### Cancellation Rules
- **Under $200**: Approve requested amount
- **Over $200**: Cap at $200
- **Gold bonus**: +$50 for gold members

### Luggage Issues Rules
- **Basic tier**: $75
- **Silver tier**: $125
- **Gold tier**: $200

### Universal Rules
- **Hard cap**: Maximum $500 compensation regardless of other rules

## API Endpoints

### WebSocket
```
wss://<your-route>/websocket-chat
```
- Real-time bidirectional communication
- Maintains persistent connection
- Automatic state management per connection

### REST
```
POST https://<your-route>/chat
Content-Type: text/plain

<message text>
```
- Stateless HTTP requests
- Simple session management (demo uses single "rest-session" ID)

## Data Flow

1. **User sends message** via WebSocket or REST
2. **Endpoint checks claim mode** - is user filing a claim?
3. **If claim mode:**
   - Extract data using regex patterns
   - Store in `CompensationState`
   - Check if all required fields collected (flight#, issue type, duration, amount, loyalty)
4. **If incomplete:**
   - Forward to Llama LLM for conversational response
5. **If complete:**
   - Invoke `FlighCompensationEndPoint.flightCompensation()`
   - Drools evaluates against business rules
   - Return approved amount with explanation

## Regex Extractors

### Flight Number
```regex
([A-Z]{2}\d{2,4})|(\d{2,4})
```
Matches: UA333, WE777, 388

### Duration
```regex
(\d+)\s*(?:hour|hr|h|day)s?
```
Matches: 3 hours, 5h, 2 days
Fallback: Bare numbers 1-72

### Compensation Amount
```regex
\$?([0-9,]+)(?:\s*dollars?)?
```
Matches: $500, 1000, 1,234 dollars
Validation: $1 - $10,000

### Loyalty Status
Simple keyword matching: "gold", "silver", "basic"

### Issue Type
Keywords: "delay", "cancel", "luggage", "baggage", "lost", "damaged"
Maps to: "delay", "cancellation", "luggage issues"

## Testing on OpenShift

### REST Endpoint
```bash
ROUTE=$(oc get route drools-quarkus-airline -o jsonpath='{.spec.host}')
curl -X POST https://$ROUTE/chat \
  -H "Content-Type: text/plain" \
  -d "I had a flight delay"
```

### WebSocket Testing
Use browser console:
```javascript
const ws = new WebSocket('wss://your-route/websocket-chat');
ws.onmessage = (e) => console.log(e.data);
ws.send('My flight UA333 was delayed 3 hours');
```

## Logging

View application logs in OpenShift:
```bash
# Follow logs in real-time
oc logs -f deployment/drools-quarkus-airline

# View recent logs
oc logs deployment/drools-quarkus-airline --tail=100
```

Logs show:
- State transitions (claim mode on/off)
- Extracted data fields
- Drools rule firing with matched rules
- LLM API calls and responses

## Troubleshooting

### Deployment Fails
```bash
# Check build logs
oc logs -f bc/drools-quarkus-airline

# Check events
oc get events --sort-by='.lastTimestamp'

# Describe deployment
oc describe deployment drools-quarkus-airline
```

### "UnsupportedOperationException" from Drools
- Ensure `drools-mvel` dependency is in pom.xml
- Check `kmodule.xml` exists in `src/main/resources/META-INF/`
- Verify rules syntax in `rules.drl`

### LLM API Connection Issues
- Verify `maas-client.url` in application.properties
- Check API authentication/bearer token configuration
- Review pod logs: `oc logs deployment/drools-quarkus-airline`

### Rules Not Firing
- Check logs for "Fired X rules" messages
- Verify all 5 fields are populated (not null) in logs
- Review rule conditions in `rules.drl`
- Check `CompensationState` logging output

### WebSocket Connection Fails
- Verify OpenShift route allows WebSocket connections
- Check pod status: `oc get pods`
- Review route configuration: `oc get route drools-quarkus-airline -o yaml`
- Ensure using `wss://` (secure WebSocket) not `ws://`

### Static Files Not Serving
- Verify HTML/JS files are in `src/main/resources/META-INF/resources/`
- Rebuild and redeploy
- Check file permissions in the container

## MCP-Like Design

The `@Tool` annotation on `FlighCompensationEndPoint` provides MCP metadata:
```java
@Tool(description = "Requires approval for compensation for a flight issue")
public String flightCompensation(...)
```

**Current Implementation**: Direct Java method invocation
**Future Potential**: LLM-driven tool discovery and invocation with MCP protocol

To enable full MCP:
1. Switch to tool-calling LLM (GPT-4, Claude 3, Gemini)
2. Implement tool discovery protocol
3. Let LLM decide when to invoke tools

## Production Considerations

- **Session Management**: Implement proper user session tracking (cookies, JWT)
- **Rate Limiting**: Add request throttling
- **Authentication**: Secure endpoints with OpenShift OAuth or other auth
- **Database**: Store claim history in database (PostgreSQL, MongoDB)
- **Monitoring**: Add Prometheus metrics and Grafana dashboards
- **Secrets**: Use OpenShift Secrets for API keys and sensitive configuration
- **Horizontal Scaling**: Configure pod autoscaling based on load
- **Health Checks**: Verify liveness and readiness probes are configured

## OpenShift Resources

After deployment, your application will have:
- **Deployment**: Manages pod replicas
- **Service**: Internal cluster networking
- **Route**: External HTTPS access
- **ImageStream**: Container image management
- **BuildConfig**: Source-to-image build configuration

View all resources:
```bash
oc get all -l app.kubernetes.io/name=drools-quarkus-airline
```

## Related

See [../chatbot-ui/README.md](../chatbot-ui/README.md) for frontend static files and configuration.

## License

[Add your license here]
