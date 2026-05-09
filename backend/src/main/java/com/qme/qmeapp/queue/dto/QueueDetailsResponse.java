package com.qme.qmeapp.queue.dto;

import com.qme.qmeapp.organization.dto.OrganizationResponse;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class QueueDetailsResponse {
    private UUID entryId;
    private OrganizationResponse organization;
    private Integer queueNumber;
    private Integer positionInLine;
    private String computedStatus; // "BEING_SERVED", "NEXT_IN_LINE", "WAITING"
    private Integer currentlyServingNumber;
    private Integer estimatedWaitTime;
    private OffsetDateTime joinedAt;
}
