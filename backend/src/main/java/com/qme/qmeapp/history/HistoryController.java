package com.qme.qmeapp.history;

import com.qme.qmeapp.common.dto.ApiResponse;
import com.qme.qmeapp.history.dto.HistoryEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * GET /api/history
     * Returns the queue history for the currently authenticated customer.
     * Lists all past entries (SERVED, CANCELLED, SKIPPED) sorted by most recent first.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoryEntryResponse>>> getMyHistory(Authentication authentication) {
        List<HistoryEntryResponse> history = historyService.getMyHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
