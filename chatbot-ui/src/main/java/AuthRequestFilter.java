import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class AuthRequestFilter implements ClientRequestFilter {

    @ConfigProperty(name = "maas-api.api-key")
    String apiKey;

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
    }
}