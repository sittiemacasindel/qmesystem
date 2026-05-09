package com.qme.qmeapp.queue.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class JoinQueueResponse {
    private UUID entryId;
    private Integer queueNumber;
    private Integer positionInLine;
    private String status;
    private Integer estimatedWaitTimeMin;
    private Integer estimatedWaitTimeMax;
}
