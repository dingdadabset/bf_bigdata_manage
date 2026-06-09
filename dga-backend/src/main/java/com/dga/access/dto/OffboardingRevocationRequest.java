package com.dga.access.dto;

import java.time.LocalDate;

public class OffboardingRevocationRequest {

    private String username;
    private String cluster;
    private LocalDate departureDate;
    private String managerContact;
    private String securityContact;
    private String reason;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public String getManagerContact() {
        return managerContact;
    }

    public void setManagerContact(String managerContact) {
        this.managerContact = managerContact;
    }

    public String getSecurityContact() {
        return securityContact;
    }

    public void setSecurityContact(String securityContact) {
        this.securityContact = securityContact;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
