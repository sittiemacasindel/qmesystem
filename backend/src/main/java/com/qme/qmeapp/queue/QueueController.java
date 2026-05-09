package com.qme.qmeapp.queue;

import com.qme.qmeapp.common.dto.ApiResponse;
import com.qme.qmeapp.queue.dto.AdminQueueResponse;
import com.qme.qmeapp.queue.dto.JoinQueueResponse;
import com.qme.qmeapp.queue.dto.QueueDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    // --- Customer Endpoints ---

    @PostMapping("/join/{queueCode}")
    public ResponseEntity<ApiResponse<JoinQueueResponse>> joinQueue(
            Authentication authentication,
            @PathVariable String queueCode) {
        JoinQueueResponse response = queueService.joinQueue(authentication.getName(), queueCode);
        return ResponseEntity.ok(ApiResponse.success("Successfully joined the queue", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<QueueDetailsResponse>> getMyQueue(Authentication authentication) {
        QueueDetailsResponse response = queueService.getMyQueueDetails(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{entryId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelMyQueue(
            Authentication authentication,
            @PathVariable UUID entryId) {
        queueService.cancelMyQueue(entryId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Queue cancelled successfully", null));
    }

    // --- Admin Endpoints ---

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<ApiResponse<List<AdminQueueResponse>>> getOrganizationQueues(
            Authentication authentication,
            @PathVariable UUID orgId) {
        List<AdminQueueResponse> response = queueService.getOrganizationQueues(orgId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/organization/{orgId}/call-next")
    public ResponseEntity<ApiResponse<Void>> callNext(
            Authentication authentication,
            @PathVariable UUID orgId) {
        queueService.callNext(orgId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Called next customer", null));
    }

    @PostMapping("/organization/{orgId}/skip")
    public ResponseEntity<ApiResponse<Void>> skipCurrent(
            Authentication authentication,
            @PathVariable UUID orgId) {
        queueService.skipCurrent(orgId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Skipped current customer", null));
    }

    @PostMapping("/entries/{entryId}/serve")
    public ResponseEntity<ApiResponse<Void>> markAsServed(
            Authentication authentication,
            @PathVariable UUID entryId) {
        queueService.markAsServed(entryId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Marked as served", null));
    }
}
