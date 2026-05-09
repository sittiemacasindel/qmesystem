package com.qme.qmeapp.organization.dto;

import lombok.Data;

@Data
public class UpdateOrgRequest {
    private String name;
    private String openingHours;
    private String closingHours;
    private Integer waitTimeMin;
    private Integer waitTimeMax;
    private String location;
    private String contactNumber;
}
