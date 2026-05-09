package com.qme.qmeapp.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsResponse {
    private long activeOrganizations;
    private long totalWaitingCustomers;
    private long totalServedToday;
}
