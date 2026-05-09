package com.qme.qmeapp.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String organization;
}
