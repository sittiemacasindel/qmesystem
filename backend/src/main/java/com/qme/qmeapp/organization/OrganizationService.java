package com.qme.qmeapp.organization;

import com.qme.qmeapp.common.exception.ResourceNotFoundException;
import com.qme.qmeapp.common.exception.UnauthorizedException;
import com.qme.qmeapp.organization.dto.CreateOrgRequest;
import com.qme.qmeapp.organization.dto.OrgStatusRequest;
import com.qme.qmeapp.organization.dto.OrganizationResponse;
import com.qme.qmeapp.organization.dto.UpdateOrgRequest;
import com.qme.qmeapp.queue.QueueEntryRepository;
import com.qme.qmeapp.user.User;
import com.qme.qmeapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final QueueEntryRepository queueEntryRepository;

    @Transactional
    public OrganizationResponse createOrganization(String adminEmail, CreateOrgRequest request) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        String queueCode = generateUniqueQueueCode();

        Organization organization = Organization.builder()
                .admin(admin)
                .name(request.getName())
                .queueCode(queueCode)
                .openingHours(request.getOpeningHours())
                .closingHours(request.getClosingHours())
                .waitTimeMin(request.getWaitTimeMin())
                .waitTimeMax(request.getWaitTimeMax())
                .location(request.getLocation())
                .contactNumber(request.getContactNumber())
                .status("ACTIVE")
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        return mapToResponse(savedOrganization);
    }

    public List<OrganizationResponse> getAdminOrganizations(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        return organizationRepository.findByAdminId(admin.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrganizationResponse getOrganizationDetails(UUID id, String adminEmail) {
        Organization org = getOrganizationIfOwnedByAdmin(id, adminEmail);
        
        OrganizationResponse response = mapToResponse(org);
        
        // Populate stats
        long waitingCount = queueEntryRepository.countByOrganizationIdAndStatus(id, "WAITING");
        long servingCount = queueEntryRepository.countByOrganizationIdAndStatus(id, "SERVING");
        response.setTotalWaitingCustomers(waitingCount + servingCount); 
        
        OffsetDateTime startOfDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long servedTodayCount = queueEntryRepository.countByOrganizationIdAndStatusAndServedAtAfter(id, "SERVED", startOfDay);
        response.setTotalServedToday(servedTodayCount);
        
        return response;
    }

    public OrganizationResponse getOrganizationByQueueCode(String queueCode) {
        Organization org = organizationRepository.findByQueueCode(queueCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with queue code: " + queueCode));
        return mapToResponse(org);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, String adminEmail, UpdateOrgRequest request) {
        Organization org = getOrganizationIfOwnedByAdmin(id, adminEmail);

        if (request.getName() != null) org.setName(request.getName());
        if (request.getOpeningHours() != null) org.setOpeningHours(request.getOpeningHours());
        if (request.getClosingHours() != null) org.setClosingHours(request.getClosingHours());
        if (request.getWaitTimeMin() != null) org.setWaitTimeMin(request.getWaitTimeMin());
        if (request.getWaitTimeMax() != null) org.setWaitTimeMax(request.getWaitTimeMax());
        if (request.getLocation() != null) org.setLocation(request.getLocation());
        if (request.getContactNumber() != null) org.setContactNumber(request.getContactNumber());

        Organization updatedOrg = organizationRepository.save(org);
        return mapToResponse(updatedOrg);
    }

    @Transactional
    public OrganizationResponse updateOrganizationStatus(UUID id, String adminEmail, OrgStatusRequest request) {
        Organization org = getOrganizationIfOwnedByAdmin(id, adminEmail);
        org.setStatus(request.getStatus().toUpperCase());
        Organization updatedOrg = organizationRepository.save(org);
        return mapToResponse(updatedOrg);
    }

    @Transactional
    public void deleteOrganization(UUID id, String adminEmail) {
        Organization org = getOrganizationIfOwnedByAdmin(id, adminEmail);
        organizationRepository.delete(org);
    }

    private Organization getOrganizationIfOwnedByAdmin(UUID id, String adminEmail) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));

        if (!org.getAdmin().getEmail().equals(adminEmail)) {
            throw new UnauthorizedException("You do not have permission to access this organization.");
        }
        return org;
    }

    private String generateUniqueQueueCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (organizationRepository.existsByQueueCode(code));
        return code;
    }

    private OrganizationResponse mapToResponse(Organization org) {
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
                .createdAt(org.getCreatedAt())
                .build();
    }
}
