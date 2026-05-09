package com.qme.qmeapp.organization;

import com.qme.qmeapp.common.dto.ApiResponse;
import com.qme.qmeapp.organization.dto.CreateOrgRequest;
import com.qme.qmeapp.organization.dto.OrgStatusRequest;
import com.qme.qmeapp.organization.dto.OrganizationResponse;
import com.qme.qmeapp.organization.dto.UpdateOrgRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            Authentication authentication,
            @Valid @RequestBody CreateOrgRequest request) {
        OrganizationResponse response = organizationService.createOrganization(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Organization created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getMyOrganizations(Authentication authentication) {
        List<OrganizationResponse> organizations = organizationService.getAdminOrganizations(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationDetails(
            Authentication authentication,
            @PathVariable UUID id) {
        OrganizationResponse response = organizationService.getOrganizationDetails(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{queueCode}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationByCode(
            @PathVariable String queueCode) {
        OrganizationResponse response = organizationService.getOrganizationByQueueCode(queueCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody UpdateOrgRequest request) {
        OrganizationResponse response = organizationService.updateOrganization(id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganizationStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody OrgStatusRequest request) {
        OrganizationResponse response = organizationService.updateOrganizationStatus(id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Organization status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            Authentication authentication,
            @PathVariable UUID id) {
        organizationService.deleteOrganization(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully", null));
    }
}
