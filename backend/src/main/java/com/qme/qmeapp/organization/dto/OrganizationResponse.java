package com.qme.qmeapp.organization.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String queueCode;
    private String openingHours;
    private String closingHours;
    private Integer waitTimeMin;
    private Integer waitTimeMax;
    private String location;
    private String contactNumber;
    private String status;
    private OffsetDateTime createdAt;
    
    // Additional fields populated when fetching details
    private Long totalWaitingCustomers;
    private Long totalServedToday;
}
