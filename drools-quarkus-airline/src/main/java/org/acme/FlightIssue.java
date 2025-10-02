package org.acme;

public class FlightIssue {

    private String flightNumber;
    private String issueType;
    private int issueDuration;
    private double customerCompensation;
    private String customerLoyaltyStatus;
    private double approvedCompensation = 0.0; // Make this private

    public FlightIssue(String flightNumber, String issueType, int issueDuration, double customerCompensation, String customerLoyaltyStatus) {
        this.flightNumber = flightNumber;
        this.issueType = issueType;
        this.issueDuration = issueDuration;
        this.customerCompensation = customerCompensation;
        this.customerLoyaltyStatus = customerLoyaltyStatus;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public int getIssueDuration() {
        return issueDuration;
    }

    public void setIssueDuration(int issueDuration) {
        this.issueDuration = issueDuration;
    }

    public double getCustomerCompensation() {
        return customerCompensation;
    }

    public void setCustomerCompensation(double customerCompensation) {
        this.customerCompensation = customerCompensation;
    }
    
    public String getCustomerLoyaltyStatus() {
        return customerLoyaltyStatus;
    }

    public void setCustomerLoyaltyStatus(String customerLoyaltyStatus) {
        this.customerLoyaltyStatus = customerLoyaltyStatus;
    }

    public double getApprovedCompensation() {
        return approvedCompensation;
    }
    
    public void setApprovedCompensation(double approvedCompensation) {
        this.approvedCompensation = approvedCompensation;
    }
}