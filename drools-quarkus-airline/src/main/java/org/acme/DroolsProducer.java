package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

@ApplicationScoped
public class DroolsProducer {
    @Inject
        KieSession kieSession;


    private final KieContainer kieContainer;

    

    public DroolsProducer() {
        KieServices kieServices = KieServices.Factory.get();
        this.kieContainer = kieServices.getKieClasspathContainer();
    }

    @Produces
    @ApplicationScoped
    public KieContainer kieContainer() {
        return kieContainer;
    }

    @Produces
    @ApplicationScoped
    public KieSession kieSession() {
        return kieContainer.newKieSession();
    }
}
