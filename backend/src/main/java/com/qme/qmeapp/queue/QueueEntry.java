package com.qme.qmeapp.queue;

import com.qme.qmeapp.organization.Organization;
import com.qme.qmeapp.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private String status; // WAITING, SERVING, SERVED, CANCELLED, SKIPPED

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "served_at")
    private OffsetDateTime servedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
}
