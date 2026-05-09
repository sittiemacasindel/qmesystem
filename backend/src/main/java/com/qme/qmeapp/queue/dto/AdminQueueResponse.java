package com.qme.qmeapp.queue.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminQueueResponse {
    private UUID entryId;
    private Integer queueNumber;
    private String customerName;
    private Integer position;
    private String status; // WAITING, SERVING
    private OffsetDateTime joinedAt;
}
