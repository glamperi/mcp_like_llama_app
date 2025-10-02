package org.acme;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "maas-api")
@RegisterProvider(MaasClientRequestFilter.class)
@Path("/v1")
public interface MaasClient {

    @POST
    @Path("/chat/completions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<MaasChatResponse> getChatCompletion(MaasChatRequest request);
}