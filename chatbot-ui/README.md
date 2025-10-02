# Chatbot UI

Quarkus-based frontend application that serves the Flight Compensation Chatbot user interface. Provides two separate implementations: WebSocket-based and REST-based chat interfaces that communicate with the backend drools-quarkus-airline service.

## Technology Stack

- **Quarkus** - Java framework for the frontend server
- **HTML, CSS, JavaScript** - Static UI files served by Quarkus
- **WebSocket API** - Real-time communication with backend
- **Fetch API** - REST communication with backend

## Files

- **`index.html`** - WebSocket-based chat interface (uses `script.js`)
- **`rest.html`** - REST-based chat interface with inline JavaScript
- **`script.js`** - WebSocket client logic for real-time bidirectional communication
- **`pom.xml`** - Maven build configuration
- **Java source files** - Quarkus application code for serving static files and proxying requests

## Features

- Serves static chat UI files
- Clean, responsive chat interface
- Real-time messaging via WebSocket
- Alternative REST implementation
- Auto-scroll to latest messages
- Enter key to send messages
- Visual feedback during message sending

## Prerequisites

- **Java**: 21 or higher
- **Maven**: 3.8+
- **OpenShift CLI**: `oc` command line tool
- **OpenShift Project**: Active project/namespace
- **Backend Service**: drools-quarkus-airline must be deployed and accessible

## Configuration

Update `src/main/resources/application.properties` to point to your backend service:

```properties
# Backend service URL (update with your backend route)
backend.service.url=https://drools-quarkus-airline-your-project.apps.cluster.com

# Or if both apps in same OpenShift project, use service name
backend.service.url=http://drools-quarkus-airline:8080
```

Update the frontend JavaScript files to use the correct backend URL:

**Edit `src/main/resources/META-INF/resources/script.js`** for WebSocket:
```javascript
const ws = new WebSocket('wss://drools-quarkus-airline-your-project.apps.cluster.com/websocket-chat');
```

**Edit `src/main/resources/META-INF/resources/rest.html`** for REST:
```javascript
const BACKEND_URL = 'https://drools-quarkus-airline-your-project.apps.cluster.com/chat';
```

## Build & Deploy to OpenShift

### Step 1: Login to OpenShift

```bash
oc login --server=https://your-openshift-cluster:6443
```

### Step 2: Create or Select Project

```bash
# Create new project
oc new-project drools-airline

# Or switch to existing project
oc project drools-airline
```

### Step 3: Build and Deploy

From the `chatbot-ui` directory:

```bash
./mvnw clean package -Dquarkus.openshift.deploy=true \
  -Dquarkus.kubernetes-client.trust-certs=true \
  -Dquarkus.openshift.route.expose=true
```

This command will:
- Clean and build the Quarkus frontend application
- Create a container image
- Deploy to your OpenShift project
- Create a Route (publicly accessible URL)
- Trust cluster certificates

### Step 4: Verify Deployment

```bash
# Check pod status
oc get pods

# Check route (get the URL)
oc get route

# View application logs
oc logs -f deployment/chatbot-ui
```

### Step 5: Access the Application

Get your route URL:
```bash
oc get route chatbot-ui -o jsonpath='{.spec.host}'
```

Access the UIs at:
- **WebSocket UI**: `https://<route-host>/index.html`
- **REST UI**: `https://<route-host>/rest.html`

## Local Development

For local testing:

```bash
cd chatbot-ui
./mvnw quarkus:dev
```

Access locally at:
- WebSocket: http://localhost:8080/index.html
- REST: http://localhost:8080/rest.html

Make sure the backend service (drools-quarkus-airline) is also running and accessible.

## Project Structure

```
chatbot-ui/
├── src/main/
│   ├── java/                           # Quarkus Java source files
│   └── resources/
│       ├── application.properties      # Configuration
│       └── META-INF/resources/         # Static files
│           ├── index.html
│           ├── rest.html
│           └── script.js
├── pom.xml                             # Maven dependencies
└── README.md
```

## Chat Flow

1. User types message and hits Enter or clicks Send
2. Frontend displays user message immediately
3. Message sent to backend (drools-quarkus-airline) via WebSocket or REST
4. Backend processes with Llama LLM and/or Drools rules engine
5. Response returned to frontend
6. Frontend displays response in chat window

## Switching Between Versions

The two UIs provide links to switch between implementations:
- WebSocket UI → Click "Use REST Version"
- REST UI → Click "Use WebSocket Version"

## Styling

Both interfaces use inline CSS with:
- Green color scheme for airline branding
- Responsive 400px width container
- Scrollable chat history
- Fixed input area at bottom

## Browser Compatibility

- Chrome/Edge: ✅ Full support
- Firefox: ✅ Full support
- Safari: ✅ Full support

## Troubleshooting

### Frontend pod not starting
```bash
# Check pod status
oc get pods

# View pod logs
oc logs deployment/chatbot-ui

# Check events
oc get events --sort-by='.lastTimestamp'

# Describe deployment
oc describe deployment chatbot-ui
```

### WebSocket connection fails
- Verify backend (drools-quarkus-airline) pod is running: `oc get pods -l app=drools-quarkus-airline`
- Check browser console for connection errors
- Ensure WebSocket URL in script.js matches backend route
- Verify using `wss://` (secure WebSocket) for HTTPS routes
- Check backend route allows WebSocket connections

### REST requests fail
- Check Network tab in browser DevTools
- Verify BACKEND_URL in rest.html matches backend route
- Check backend pod logs: `oc logs deployment/drools-quarkus-airline`
- Verify CORS is enabled on backend

### UI not loading
- Verify frontend route is accessible: `curl https://<frontend-route>/index.html`
- Check frontend pod logs: `oc logs deployment/chatbot-ui`
- Verify static files are in `src/main/resources/META-INF/resources/`

### Can't connect to backend
- Verify backend service is running: `oc get svc drools-quarkus-airline`
- Test backend route directly: `curl https://<backend-route>/chat`
- Check network policies if services can't communicate
- Verify backend route in application.properties or JS files

### Deployment fails
```bash
# Check build logs
oc logs -f bc/chatbot-ui

# Check build config
oc describe bc chatbot-ui

# View all resources
oc get all -l app.kubernetes.io/name=chatbot-ui
```

## OpenShift Resources

After deployment, check your resources:

```bash
# View all frontend application resources
oc get all -l app.kubernetes.io/name=chatbot-ui

# Check route details
oc describe route chatbot-ui

# Monitor pod status
oc get pods -w

# Check if both frontend and backend are running
oc get pods
```

## Redeployment

To update the UI after making changes:

```bash
# Rebuild and redeploy
./mvnw clean package -Dquarkus.openshift.deploy=true \
  -Dquarkus.kubernetes-client.trust-certs=true \
  -Dquarkus.openshift.route.expose=true

# Or trigger a new build from existing image
oc start-build chatbot-ui
```

## Communication Between Services

This frontend app communicates with the drools-quarkus-airline backend:

- **WebSocket**: `wss://backend-route/websocket-chat`
- **REST**: `https://backend-route/chat`

Both apps can be in the same OpenShift project or different projects. Update the URLs in the JavaScript files accordingly.

## Related

See [../drools-quarkus-airline/README.md](../drools-quarkus-airline/README.md) for backend service documentation, Drools rules, and API endpoints.

## Production Considerations

- Configure proper service-to-service communication
- Set resource limits and requests for pods
- Enable horizontal pod autoscaling based on traffic
- Use ConfigMaps for environment-specific URLs
- Monitor both frontend and backend with OpenShift metrics
- Implement health checks (liveness/readiness probes)
- Secure inter-service communication if needed
