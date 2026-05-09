package com.qme.qmeapp.organization.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrgRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Opening hours are required")
    private String openingHours;

    @NotBlank(message = "Closing hours are required")
    private String closingHours;

    @NotNull(message = "Minimum wait time is required")
    @Min(0)
    private Integer waitTimeMin;

    @NotNull(message = "Maximum wait time is required")
    @Min(0)
    private Integer waitTimeMax;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Contact number is required")
    private String contactNumber;
}
