package com.qme.qmeapp.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByAdminId(UUID adminId);
    Optional<Organization> findByQueueCode(String queueCode);
    boolean existsByQueueCode(String queueCode);
    long countByStatus(String status);
}
