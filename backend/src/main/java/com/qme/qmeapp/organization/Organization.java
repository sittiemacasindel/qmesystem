package com.qme.qmeapp.organization;

import com.qme.qmeapp.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false)
    private String name;

    @Column(name = "queue_code", nullable = false, unique = true, length = 6)
    private String queueCode;

    @Column(name = "opening_hours", nullable = false)
    private String openingHours;

    @Column(name = "closing_hours", nullable = false)
    private String closingHours;

    @Column(name = "wait_time_min", nullable = false)
    private Integer waitTimeMin;

    @Column(name = "wait_time_max", nullable = false)
    private Integer waitTimeMax;

    @Column(nullable = false)
    private String location;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(nullable = false)
    private String status; // ACTIVE or PAUSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
