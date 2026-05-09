package com.qme.qmeapp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrgStatusRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|PAUSED", message = "Status must be ACTIVE or PAUSED")
    private String status;
}
