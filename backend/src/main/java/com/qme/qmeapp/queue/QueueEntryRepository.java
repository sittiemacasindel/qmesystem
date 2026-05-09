package com.qme.qmeapp.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

    List<QueueEntry> findByOrganizationIdAndStatusInOrderByPositionAsc(UUID organizationId, List<String> statuses);

    Optional<QueueEntry> findByCustomerIdAndStatusIn(UUID customerId, List<String> statuses);

    boolean existsByOrganizationIdAndQueueNumber(UUID organizationId, Integer queueNumber);

    long countByOrganizationIdAndStatus(UUID organizationId, String status);
    
    long countByOrganizationIdAndStatusAndServedAtAfter(UUID organizationId, String status, OffsetDateTime startOfDay);

    long countByStatus(String status);
    
    long countByStatusAndServedAtAfter(String status, OffsetDateTime startOfDay);

    List<QueueEntry> findByCustomerIdAndStatusNotInOrderByJoinedAtDesc(UUID customerId, List<String> statuses);
    
    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.organization.id = :orgId AND q.status = 'WAITING' AND q.position < :position")
    long countWaitingBeforePosition(@Param("orgId") UUID orgId, @Param("position") Integer position);

    Optional<QueueEntry> findFirstByOrganizationIdAndStatusOrderByPositionAsc(UUID organizationId, String status);
}
