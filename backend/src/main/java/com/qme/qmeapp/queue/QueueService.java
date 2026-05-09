package com.qme.qmeapp.queue;

import com.qme.qmeapp.common.exception.BadRequestException;
import com.qme.qmeapp.common.exception.ResourceNotFoundException;
import com.qme.qmeapp.common.exception.UnauthorizedException;
import com.qme.qmeapp.organization.Organization;
import com.qme.qmeapp.organization.OrganizationRepository;
import com.qme.qmeapp.organization.dto.OrganizationResponse;
import com.qme.qmeapp.queue.dto.AdminQueueResponse;
import com.qme.qmeapp.queue.dto.JoinQueueResponse;
import com.qme.qmeapp.queue.dto.QueueDetailsResponse;
import com.qme.qmeapp.user.User;
import com.qme.qmeapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public JoinQueueResponse joinQueue(String customerEmail, String queueCode) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        Organization org = organizationRepository.findByQueueCode(queueCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with code: " + queueCode));

        if (!"ACTIVE".equals(org.getStatus())) {
            throw new BadRequestException("This queue is currently not active.");
        }

        // Check if customer is already in an active queue for ANY organization (or just this one)
        // Rule: A customer can only be in one active queue at a time
        Optional<QueueEntry> existingEntry = queueEntryRepository.findByCustomerIdAndStatusIn(
                customer.getId(), List.of("WAITING", "SERVING"));
        if (existingEntry.isPresent()) {
            throw new BadRequestException("You are already in an active queue.");
        }

        Integer queueNumber = generateUniqueQueueNumber(org.getId());
        
        // Calculate initial position: max position + 1, or simply base it on how many are currently waiting/serving
        List<QueueEntry> activeEntries = queueEntryRepository.findByOrganizationIdAndStatusInOrderByPositionAsc(
                org.getId(), List.of("WAITING", "SERVING"));
        
        int newPosition = 1;
        if (!activeEntries.isEmpty()) {
            newPosition = activeEntries.get(activeEntries.size() - 1).getPosition() + 1;
        }

        QueueEntry entry = QueueEntry.builder()
                .organization(org)
                .customer(customer)
                .queueNumber(queueNumber)
                .position(newPosition)
                .status("WAITING")
                .build();

        QueueEntry savedEntry = queueEntryRepository.save(entry);

        long waitingCount = queueEntryRepository.countWaitingBeforePosition(org.getId(), savedEntry.getPosition());
        int positionInLine = (int) waitingCount + 1;

        return JoinQueueResponse.builder()
                .entryId(savedEntry.getId())
                .queueNumber(savedEntry.getQueueNumber())
                .positionInLine(positionInLine)
                .status("WAITING")
                .estimatedWaitTimeMin((positionInLine - 1) * org.getWaitTimeMin())
                .estimatedWaitTimeMax((positionInLine - 1) * org.getWaitTimeMax())
                .build();
    }

    public QueueDetailsResponse getMyQueueDetails(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        QueueEntry entry = queueEntryRepository.findByCustomerIdAndStatusIn(
                customer.getId(), List.of("WAITING", "SERVING"))
                .orElseThrow(() -> new ResourceNotFoundException("You are not currently in any queue."));

        Organization org = entry.getOrganization();

        long waitingCount = queueEntryRepository.countWaitingBeforePosition(org.getId(), entry.getPosition());
        int positionInLine = "SERVING".equals(entry.getStatus()) ? 1 : (int) waitingCount + (hasServingEntry(org.getId()) ? 2 : 1);

        String computedStatus = getComputedStatus(entry.getStatus(), positionInLine);
        
        Optional<QueueEntry> servingEntry = queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(org.getId(), "SERVING");
        Integer currentlyServingNumber = servingEntry.map(QueueEntry::getQueueNumber).orElse(null);

        // Average wait time estimate
        int avgWaitTime = (org.getWaitTimeMin() + org.getWaitTimeMax()) / 2;
        int estimatedWaitTime = Math.max(0, (positionInLine - 1) * avgWaitTime);

        return QueueDetailsResponse.builder()
                .entryId(entry.getId())
                .organization(mapToOrgResponse(org))
                .queueNumber(entry.getQueueNumber())
                .positionInLine(positionInLine)
                .computedStatus(computedStatus)
                .currentlyServingNumber(currentlyServingNumber)
                .estimatedWaitTime(estimatedWaitTime)
                .joinedAt(entry.getJoinedAt())
                .build();
    }

    @Transactional
    public void cancelMyQueue(UUID entryId, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found."));

        if (!entry.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedException("This is not your queue entry.");
        }

        if (!List.of("WAITING", "SERVING").contains(entry.getStatus())) {
            throw new BadRequestException("Can only cancel active queue entries.");
        }

        entry.setStatus("CANCELLED");
        entry.setCancelledAt(OffsetDateTime.now());
        queueEntryRepository.save(entry);
    }

    // --- Admin Operations ---

    public List<AdminQueueResponse> getOrganizationQueues(UUID orgId, String adminEmail) {
        verifyOrgAdmin(orgId, adminEmail);

        List<QueueEntry> entries = queueEntryRepository.findByOrganizationIdAndStatusInOrderByPositionAsc(
                orgId, List.of("SERVING", "WAITING"));

        return entries.stream().map(entry -> {
            long waitingCount = queueEntryRepository.countWaitingBeforePosition(orgId, entry.getPosition());
            int positionInLine = "SERVING".equals(entry.getStatus()) ? 1 : (int) waitingCount + (hasServingEntry(orgId) ? 2 : 1);

            return AdminQueueResponse.builder()
                    .entryId(entry.getId())
                    .queueNumber(entry.getQueueNumber())
                    .customerName(entry.getCustomer().getName())
                    .position(positionInLine)
                    .status(entry.getStatus())
                    .joinedAt(entry.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void callNext(UUID orgId, String adminEmail) {
        verifyOrgAdmin(orgId, adminEmail);

        // 1. Mark current SERVING as SERVED
        Optional<QueueEntry> servingEntry = queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(orgId, "SERVING");
        servingEntry.ifPresent(entry -> {
            entry.setStatus("SERVED");
            entry.setServedAt(OffsetDateTime.now());
            queueEntryRepository.save(entry);
        });

        // 2. Find next WAITING and mark as SERVING
        Optional<QueueEntry> nextEntry = queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(orgId, "WAITING");
        nextEntry.ifPresent(entry -> {
            entry.setStatus("SERVING");
            queueEntryRepository.save(entry);
        });
    }

    @Transactional
    public void skipCurrent(UUID orgId, String adminEmail) {
        verifyOrgAdmin(orgId, adminEmail);

        // 1. Mark current SERVING as SKIPPED
        Optional<QueueEntry> servingEntry = queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(orgId, "SERVING");
        servingEntry.ifPresent(entry -> {
            entry.setStatus("SKIPPED");
            queueEntryRepository.save(entry);
        });

        // 2. Find next WAITING and mark as SERVING
        Optional<QueueEntry> nextEntry = queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(orgId, "WAITING");
        nextEntry.ifPresent(entry -> {
            entry.setStatus("SERVING");
            queueEntryRepository.save(entry);
        });
    }

    @Transactional
    public void markAsServed(UUID entryId, String adminEmail) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found."));

        verifyOrgAdmin(entry.getOrganization().getId(), adminEmail);

        if (!List.of("WAITING", "SERVING").contains(entry.getStatus())) {
            throw new BadRequestException("Only active entries can be marked as served.");
        }

        entry.setStatus("SERVED");
        entry.setServedAt(OffsetDateTime.now());
        queueEntryRepository.save(entry);
    }

    // --- Helper Methods ---

    private void verifyOrgAdmin(UUID orgId, String adminEmail) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));

        if (!org.getAdmin().getEmail().equals(adminEmail)) {
            throw new UnauthorizedException("You do not have permission to manage this organization's queue.");
        }
    }

    private Integer generateUniqueQueueNumber(UUID orgId) {
        Random rnd = new Random();
        Integer number;
        do {
            number = 1000 + rnd.nextInt(9000); // 1000 to 9999
        } while (queueEntryRepository.existsByOrganizationIdAndQueueNumber(orgId, number));
        return number;
    }

    private boolean hasServingEntry(UUID orgId) {
        return queueEntryRepository.findFirstByOrganizationIdAndStatusOrderByPositionAsc(orgId, "SERVING").isPresent();
    }

    private String getComputedStatus(String actualStatus, int positionInLine) {
        if ("SERVING".equals(actualStatus)) return "BEING_SERVED";
        if (positionInLine == 1) return "BEING_SERVED"; // Fallback if no one is explicitly serving yet but they are 1st
        if (positionInLine == 2) return "NEXT_IN_LINE";
        return "WAITING";
    }

    private OrganizationResponse mapToOrgResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .queueCode(org.getQueueCode())
                .openingHours(org.getOpeningHours())
                .closingHours(org.getClosingHours())
                .waitTimeMin(org.getWaitTimeMin())
                .waitTimeMax(org.getWaitTimeMax())
                .location(org.getLocation())
                .contactNumber(org.getContactNumber())
                .status(org.getStatus())
                .build();
    }
}
