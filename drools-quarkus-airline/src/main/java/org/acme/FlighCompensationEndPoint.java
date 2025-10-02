package org.acme;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.jboss.logging.Logger;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

@Path("/compensation")
public class FlighCompensationEndPoint {
    
    private static final Logger LOG = Logger.getLogger(FlighCompensationEndPoint.class);
    
    private final class RuleListener extends DefaultAgendaEventListener {
        private List<String> matchedRules = new ArrayList<>();
        
        private RuleListener() {}
        
        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            matchedRules.add(event.getMatch().getRule().getName());
        }
        
        public List<String> getMatchedRule() {
            return matchedRules;
        }
    }

    @Inject
    KieContainer kieContainer;

    @Tool(description = "Requires approval for compensation for a flight issue")
    public String flightCompensation(
        @ToolArg(description = "The flight number of flight which the requesting compensation for") String flightNumber,
        @ToolArg(description = "The issue, valid issues are delay, cancellation, lost luggage") String issueType,
        @ToolArg(description = "How long the delay lasted in hours or days") int issueDuration, 
        @ToolArg(description = "The initial compensation") double customerCompensation,
        @ToolArg(description = "The Customer Loyalty Tier: basic, silver, gold") String customerLoyaltyStatus) {
        
        KieSession kieSession = null;
        try {
            LOG.info("=== Starting compensation processing ===");
            LOG.info("Flight: " + flightNumber + ", Issue: " + issueType + ", Duration: " + issueDuration + 
                    ", Compensation: " + customerCompensation + ", Loyalty: " + customerLoyaltyStatus);
            
            LOG.info("Creating KieSession...");
            
            // Try multiple ways to get a session
            try {
                kieSession = kieContainer.newKieSession();
                LOG.info("✓ Created default KieSession successfully");
            } catch (Exception e1) {
                LOG.error("✗ Failed to create default session: " + e1.getMessage(), e1);
                try {
                    kieSession = kieContainer.getKieBase("rules").newKieSession();
                    LOG.info("✓ Created KieSession from KieBase successfully");
                } catch (Exception e2) {
                    LOG.error("✗ Failed to create session from KieBase: " + e2.getMessage(), e2);
                    throw new RuntimeException("Unable to create KieSession: " + e2.getMessage(), e2);
                }
            }
            
            LOG.info("Creating FlightIssue object...");
            FlightIssue issue = new FlightIssue(flightNumber, issueType, issueDuration, customerCompensation, customerLoyaltyStatus);
            LOG.info("✓ FlightIssue created with initial approvedCompensation: " + issue.getApprovedCompensation());
            
            LOG.info("Adding rule listener...");
            RuleListener ruleListener = new RuleListener();
            kieSession.addEventListener(ruleListener);
            LOG.info("✓ Rule listener added");
            
            LOG.info("Inserting fact into session...");
            kieSession.insert(issue);
            LOG.info("✓ Fact inserted");
            
            LOG.info("Firing all rules...");
            int rulesFired = kieSession.fireAllRules();
            LOG.info("✓ Fired " + rulesFired + " rule(s)");
            LOG.info("Rules that matched: " + ruleListener.getMatchedRule());
            
            LOG.info("Final approvedCompensation: " + issue.getApprovedCompensation());
            
            if (issue.getApprovedCompensation() > 0) {
                String result = "Approved compensation of $" + issue.getApprovedCompensation() + " for flight " + issue.getFlightNumber()
                        + "\nRules applied: " + ruleListener.getMatchedRule();
                LOG.info("✓ Returning success: " + result);
                return result;
            } else {
                String result = "No compensation approved for flight " + issue.getFlightNumber() + 
                               ". Rules fired: " + rulesFired + ", Matched rules: " + ruleListener.getMatchedRule();
                LOG.info("✓ Returning no compensation: " + result);
                return result;
            }
        } catch (Exception e) {
            LOG.error("✗ Error in flightCompensation: " + e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Error processing compensation: " + e.getMessage(), e);
        } finally {
            if (kieSession != null) {
                try {
                    kieSession.dispose();
                    LOG.info("✓ KieSession disposed");
                } catch (Exception e) {
                    LOG.error("✗ Error disposing session: " + e.getMessage(), e);
                }
            }
            LOG.info("=== Finished compensation processing ===");
        }
    }
}