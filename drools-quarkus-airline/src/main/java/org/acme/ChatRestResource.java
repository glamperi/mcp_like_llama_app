package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Path("/chat")
public class ChatRestResource {

    private static final Logger LOG = Logger.getLogger(ChatRestResource.class);
    private static final Map<String, List<MaasChatRequest.Message>> conversations = new ConcurrentHashMap<>();
    private static final Map<String, CompensationState> states = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 20;

    @RestClient
    MaasClient maasClient;
    
    @Inject
    FlighCompensationEndPoint compensationEndpoint;

    private static class CompensationState {
        String flightNumber;
        String issueType;
        Integer issueDuration;
        Double compensation;
        String loyaltyStatus;
        boolean inClaimMode = false;
        
        boolean hasAllRequiredData() {
            // Duration not required for luggage issues
            boolean durationValid = "luggage issues".equals(issueType) || issueDuration != null;
            
            return flightNumber != null && 
                   issueType != null && 
                   durationValid &&
                   compensation != null &&
                   loyaltyStatus != null;
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(String message) {
        try {
            // For REST, we'll use a session ID (could be from cookie/header in production)
            String sessionId = "rest-session"; // Simplified for demo
            
            List<MaasChatRequest.Message> history = conversations.get(sessionId);
            CompensationState state = states.get(sessionId);
            
            if (history == null) {
                history = new ArrayList<>();
                history.add(new MaasChatRequest.Message("system", 
                    "You are a helpful airline customer service agent. You assist with general flight inquiries AND compensation claims. " +
                    "DEFAULT BEHAVIOR - GENERAL CONVERSATION:\n" +
                    "- When someone says 'hi' or 'hello', respond with a friendly greeting like 'Hello! How can I help you today?'\n" +
                    "- Answer general questions about flights, booking, policies naturally\n" +
                    "- DO NOT immediately ask for flight numbers or claim details\n" +
                    "- DO NOT assume every conversation is about filing a claim\n\n" +
                    "ONLY IF they mention a flight problem (delay, cancellation, luggage issues):\n" +
                    "- Acknowledge the issue with empathy\n" +
                    "- Ask: 'Would you like help filing a compensation claim for this?'\n" +
                    "- Wait for their confirmation (yes/sure/ok)\n\n" +
                    "ONLY AFTER they confirm wanting to file:\n" +
                    "- Say: 'I'll need to collect information to process your claim.'\n" +
                    "- Collect in this order:\n" +
                    "  1. Flight number\n" +
                    "  2. Issue type (delay/cancellation/luggage issues)\n" +
                    "  3. IF delay or cancellation: ask 'How many hours was the delay/cancellation?'\n" +
                    "     IF luggage issues: SKIP duration question, go directly to step 4\n" +
                    "  4. Compensation amount - ask 'How much compensation would you like to request in dollars?'\n" +
                    "  5. Loyalty tier - ask 'What is your rewards tier: Basic, Silver, or Gold?' (MUST get one of these three words)\n" +
                    "- Ask ONE question at a time and wait for the answer\n" +
                    "- CRITICAL: Do NOT say you submitted the claim or that you'll process it - the system does that AUTOMATICALLY after you collect all pieces\n" +
                    "- After collecting loyalty tier, just say 'Thank you, I have all the information needed.' and STOP\n\n" +
                    "REMEMBER: You ONLY collect information. The backend system submits the claim automatically."));
                conversations.put(sessionId, history);
            }
            
            if (state == null) {
                state = new CompensationState();
                states.put(sessionId, state);
            }

            LOG.info("REST message: " + message);

            String lowerMessage = message.toLowerCase();

            // Check if user is agreeing to file a claim
            if (!state.inClaimMode) {
                if (lowerMessage.contains("yes") || lowerMessage.contains("sure") || 
                    lowerMessage.contains("ok") || lowerMessage.contains("file")) {
                    state.inClaimMode = true;
                    LOG.info("Switching to claim mode");
                }
            }

            // Extract information from user message if in claim mode
            if (state.inClaimMode) {
                extractCompensationInfo(message, state);
            }
            
            LOG.info("Current state - Flight: " + state.flightNumber + ", Issue: " + state.issueType + 
                    ", Duration: " + state.issueDuration + ", Compensation: " + state.compensation + 
                    ", Loyalty: " + state.loyaltyStatus + ", InClaimMode: " + state.inClaimMode);

            // If we have all data, automatically submit to Drools
            if (state.hasAllRequiredData()) {
                LOG.info("All data collected, processing compensation with Drools");
                
                try {
                    String result = compensationEndpoint.flightCompensation(
                        state.flightNumber,
                        state.issueType,
                        state.issueDuration != null ? state.issueDuration : 0, // Use 0 for luggage
                        state.compensation,
                        state.loyaltyStatus
                    );
                    
                    // Parse the result to check approved amount vs requested
                    String responseMessage;
                    if (result.contains("Approved compensation of")) {
                        Pattern pattern = Pattern.compile("Approved compensation of \\$([0-9.]+)");
                        Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            double approvedAmount = Double.parseDouble(matcher.group(1));
                            
                            if (approvedAmount > state.compensation) {
                                // They get MORE than they asked for - bonus!
                                responseMessage = "Great news! Based on your " + state.loyaltyStatus + " rewards status and the circumstances of your claim, " +
                                    "we're pleased to offer you $" + approvedAmount + " in compensation for your " + state.issueType + 
                                    " on flight " + state.flightNumber + ".\n\n" +
                                    "This is more than the $" + state.compensation + " you requested!";
                            } else if (approvedAmount == state.compensation) {
                                // Exact match
                                responseMessage = "Good news! Your compensation claim has been approved.\n\n" + 
                                    "You will receive $" + approvedAmount + " for your " + state.issueType + " on flight " + state.flightNumber + ".";
                            } else {
                                // Less than requested
                                responseMessage = "I've submitted your claim to our automated approval system.\n\n" +
                                    "Based on our policies, the approved compensation is $" + approvedAmount + " for your " + state.issueType + 
                                    " on flight " + state.flightNumber + ".\n\n" +
                                    "If you would like to discuss this further with a live customer service agent, please click the customer service icon to connect.";
                            }
                        } else {
                            responseMessage = result;
                        }
                    } else {
                        // No compensation approved
                        responseMessage = result + 
                            "\n\nIf you would like to discuss this further with a live customer service agent, please click the customer service icon to connect.";
                    }
                    
                    // Reset state after processing
                    states.put(sessionId, new CompensationState());
                    conversations.remove(sessionId);
                    
                    return responseMessage;
                    
                } catch (Exception e) {
                    LOG.error("Error calling Drools: " + e.getMessage(), e);
                    return "I encountered an error processing your compensation claim. Please try again.";
                }
            }

            // Otherwise, continue conversation with LLM
            if (history.size() > MAX_HISTORY_SIZE) {
                MaasChatRequest.Message systemMsg = history.get(0);
                history.clear();
                history.add(systemMsg);
            }

            history.add(new MaasChatRequest.Message("user", message));

            MaasChatRequest request = new MaasChatRequest(history);
            MaasChatResponse response = maasClient.getChatCompletion(request).await().indefinitely();

            if (response.choices != null && !response.choices.isEmpty()) {
                String botResponse = response.choices.get(0).message.content;
                history.add(new MaasChatRequest.Message("assistant", botResponse));
                return botResponse;
            } else {
                return "I'm sorry, I couldn't process your request at this time.";
            }

        } catch (Exception e) {
            LOG.error("REST error: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    private void extractCompensationInfo(String message, CompensationState state) {
        String lowerMessage = message.toLowerCase();
        
        Pattern flightPattern = Pattern.compile(
            "(?:flight\\s*(?:number|#)?\\s*:?\\s*)?([A-Z]{2}\\d{2,4})|(?:flight\\s*#?\\s*)(\\d{2,4})", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher flightMatcher = flightPattern.matcher(message);
        if (flightMatcher.find() && state.flightNumber == null) {
            if (flightMatcher.group(1) != null) {
                state.flightNumber = flightMatcher.group(1);
            } else if (flightMatcher.group(2) != null) {
                state.flightNumber = "FL" + flightMatcher.group(2);
            }
        }
        
        if (lowerMessage.contains("delay") && state.issueType == null) {
            state.issueType = "delay";
        } else if (lowerMessage.contains("cancel") && state.issueType == null) {
            state.issueType = "cancellation";
        } else if ((lowerMessage.contains("luggage") || lowerMessage.contains("baggage") || 
                   lowerMessage.contains("bag") || lowerMessage.contains("lost") || 
                   lowerMessage.contains("damaged") || lowerMessage.contains("missing")) && state.issueType == null) {
            state.issueType = "luggage issues";
        }
        
        // Extract duration - only for delay/cancellation, not for luggage
        if (!"luggage issues".equals(state.issueType) &&
            !lowerMessage.contains("$") && 
            !lowerMessage.contains("dollar") && 
            !lowerMessage.contains("compensation") && 
            !lowerMessage.contains("request") &&
            state.issueDuration == null) {
            
            // First try with hour/day keywords
            Pattern durationPattern = Pattern.compile("(\\d+)\\s*(?:hour|hr|h|day)s?", Pattern.CASE_INSENSITIVE);
            Matcher durationMatcher = durationPattern.matcher(message);
            if (durationMatcher.find()) {
                state.issueDuration = Integer.parseInt(durationMatcher.group(1));
            } else {
                // If no keywords, check if the message is JUST a number (for when user responds with just "3")
                String trimmed = message.trim();
                if (trimmed.matches("\\d+")) {
                    int num = Integer.parseInt(trimmed);
                    // Only accept reasonable hour values (1-72 hours)
                    if (num >= 1 && num <= 72) {
                        state.issueDuration = num;
                    }
                }
            }
        }
        
        if ((lowerMessage.contains("$") || 
             lowerMessage.contains("dollar") || 
             lowerMessage.contains("compensation") ||
             lowerMessage.contains("request") ||
             lowerMessage.contains("want") ||
             lowerMessage.contains("seeking")) && 
            state.compensation == null) {
            Pattern compensationPattern = Pattern.compile("\\$?([0-9,]+)(?:\\s*dollars?)?", Pattern.CASE_INSENSITIVE);
            Matcher compensationMatcher = compensationPattern.matcher(message);
            if (compensationMatcher.find()) {
                String amountStr = compensationMatcher.group(1).replace(",", "");
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0 && amount <= 10000) {
                        state.compensation = amount;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        if ((lowerMessage.contains("gold") || lowerMessage.contains("gold member")) && state.loyaltyStatus == null) {
            state.loyaltyStatus = "gold";
        } else if ((lowerMessage.contains("silver") || lowerMessage.contains("silver member")) && state.loyaltyStatus == null) {
            state.loyaltyStatus = "silver";
        } else if ((lowerMessage.contains("basic") || lowerMessage.contains("basic member")) && state.loyaltyStatus == null) {
            state.loyaltyStatus = "basic";
        }
    }
}