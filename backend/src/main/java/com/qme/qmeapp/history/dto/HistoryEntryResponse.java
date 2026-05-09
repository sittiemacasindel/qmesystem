package com.qme.qmeapp.history.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class HistoryEntryResponse {
    private UUID entryId;
    private UUID organizationId;
    private String organizationName;
    private String queueCode;
    private Integer queueNumber;
    private String status;       // SERVED, CANCELLED, SKIPPED
    private OffsetDateTime joinedAt;
    private OffsetDateTime completedAt; // servedAt, cancelledAt, or null
}