package com.qme.qmeapp.history;

import com.qme.qmeapp.common.exception.ResourceNotFoundException;
import com.qme.qmeapp.history.dto.HistoryEntryResponse;
import com.qme.qmeapp.queue.QueueEntry;
import com.qme.qmeapp.queue.QueueEntryRepository;
import com.qme.qmeapp.user.User;
import com.qme.qmeapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QueueEntryRepository queueEntryRepository;
    private final UserRepository userRepository;

    /**
     * Returns the queue history for the authenticated customer.
     * Only returns entries that are no longer active: SERVED, CANCELLED, or SKIPPED.
     */
    public List<HistoryEntryResponse> getMyHistory(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        // Exclude only truly active statuses; everything else is history
        List<QueueEntry> historyEntries = queueEntryRepository
                .findByCustomerIdAndStatusNotInOrderByJoinedAtDesc(
                        customer.getId(),
                        List.of("WAITING", "SERVING")
                );

        return historyEntries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private HistoryEntryResponse mapToResponse(QueueEntry entry) {
        // Determine the completed timestamp: served > cancelled > null
        OffsetDateTime completedAt = entry.getServedAt() != null
                ? entry.getServedAt()
                : entry.getCancelledAt();

        return HistoryEntryResponse.builder()
                .entryId(entry.getId())
                .organizationId(entry.getOrganization().getId())
                .organizationName(entry.getOrganization().getName())
                .queueCode(entry.getOrganization().getQueueCode())
                .queueNumber(entry.getQueueNumber())
                .status(entry.getStatus())
                .joinedAt(entry.getJoinedAt())
                .completedAt(completedAt)
                .build();
    }
}
