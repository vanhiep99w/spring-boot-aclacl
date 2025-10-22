package com.example.acl.web;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.Group;
import com.example.acl.domain.Project;
import com.example.acl.domain.Role;
import com.example.acl.service.AclPermissionRegistry;
import com.example.acl.service.AclPermissionService;
import com.example.acl.service.AclSidResolver;
import com.example.acl.service.PermissionDiscoveryService;
import com.example.acl.web.dto.AccessibleResourcesResponse;
import com.example.acl.web.dto.BulkPermissionUpdateRequest;
import com.example.acl.web.dto.EffectivePermissionsResponse;
import com.example.acl.web.dto.PermissionGrantRequest;
import com.example.acl.web.dto.PermissionInheritanceResponse;
import com.example.acl.web.dto.PermissionResponse;
import com.example.acl.web.dto.PermissionRevokeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionManagementController {

    private final AclPermissionService aclPermissionService;
    private final PermissionDiscoveryService permissionDiscoveryService;
    private final AclPermissionRegistry permissionRegistry;
    private final AclSidResolver sidResolver;

    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PermissionResponse> grantPermission(
            @Valid @RequestBody PermissionGrantRequest request) {
        
        log.info("Granting permissions {} to {} {} on {} {}",
                request.getPermissions(),
                request.getSubjectType(),
                request.getSubjectIdentifier(),
                request.getResourceType(),
                request.getResourceId());

        try {
            Class<?> domainClass = resolveDomainClass(request.getResourceType());
            Sid sid = resolveSid(request.getSubjectType(), request.getSubjectIdentifier());
            List<Permission> permissions = permissionRegistry.resolvePermissions(request.getPermissions());

            aclPermissionService.grantPermissions(domainClass, request.getResourceId(), sid, permissions);

            return ResponseEntity.ok(PermissionResponse.builder()
                    .success(true)
                    .message("Permissions granted successfully")
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .subject(request.getSubjectType() + ":" + request.getSubjectIdentifier())
                    .build());

        } catch (Exception e) {
            log.error("Error granting permissions", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(PermissionResponse.builder()
                            .success(false)
                            .message("Error granting permissions: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PermissionResponse> revokePermission(
            @Valid @RequestBody PermissionRevokeRequest request) {
        
        log.info("Revoking permissions {} from {} {} on {} {}",
                request.getPermissions(),
                request.getSubjectType(),
                request.getSubjectIdentifier(),
                request.getResourceType(),
                request.getResourceId());

        try {
            Class<?> domainClass = resolveDomainClass(request.getResourceType());
            Sid sid = resolveSid(request.getSubjectType(), request.getSubjectIdentifier());
            List<Permission> permissions = permissionRegistry.resolvePermissions(request.getPermissions());

            aclPermissionService.revokePermissions(domainClass, request.getResourceId(), sid, permissions);

            return ResponseEntity.ok(PermissionResponse.builder()
                    .success(true)
                    .message("Permissions revoked successfully")
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .subject(request.getSubjectType() + ":" + request.getSubjectIdentifier())
                    .build());

        } catch (Exception e) {
            log.error("Error revoking permissions", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(PermissionResponse.builder()
                            .success(false)
                            .message("Error revoking permissions: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkUpdatePermissions(
            @Valid @RequestBody BulkPermissionUpdateRequest request) {
        
        log.info("Bulk {} permissions {} for {} {} on {} resources",
                request.getOperation(),
                request.getPermissions(),
                request.getSubjectType(),
                request.getSubjectIdentifier(),
                request.getResourceIds().size());

        try {
            Class<?> domainClass = resolveDomainClass(request.getResourceType());
            Sid sid = resolveSid(request.getSubjectType(), request.getSubjectIdentifier());
            List<Permission> permissions = permissionRegistry.resolvePermissions(request.getPermissions());

            if ("GRANT".equalsIgnoreCase(request.getOperation())) {
                aclPermissionService.bulkGrant(domainClass, request.getResourceIds(), sid, permissions);
            } else if ("REVOKE".equalsIgnoreCase(request.getOperation())) {
                aclPermissionService.bulkRevoke(domainClass, request.getResourceIds(), sid, permissions);
            } else {
                throw new IllegalArgumentException("Invalid operation: " + request.getOperation());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bulk operation completed successfully");
            response.put("operation", request.getOperation());
            response.put("resourcesAffected", request.getResourceIds().size());
            response.put("resourceType", request.getResourceType());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error performing bulk operation", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error performing bulk operation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EffectivePermissionsResponse> checkEffectivePermissions(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            Authentication authentication) {
        
        log.info("Checking effective permissions for {} on {} {}",
                authentication.getName(), resourceType, resourceId);

        try {
            Class<?> domainClass = resolveDomainClass(resourceType);
            EffectivePermissionsResponse response = permissionDiscoveryService
                    .getEffectivePermissions(domainClass, resourceId, authentication);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking effective permissions", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/accessible")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccessibleResourcesResponse> listAccessibleResources(
            @RequestParam String resourceType,
            Authentication authentication) {
        
        log.info("Listing accessible {} for {}", resourceType, authentication.getName());

        try {
            AccessibleResourcesResponse response = permissionDiscoveryService
                    .findAccessibleResources(resourceType, authentication);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error listing accessible resources", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/inheritance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PermissionInheritanceResponse> checkInheritance(
            @RequestParam String resourceType,
            @RequestParam Long resourceId) {
        
        log.info("Checking permission inheritance for {} {}", resourceType, resourceId);

        try {
            Class<?> domainClass = resolveDomainClass(resourceType);
            PermissionInheritanceResponse response = permissionDiscoveryService
                    .getPermissionInheritance(domainClass, resourceId);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking permission inheritance", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> listAvailablePermissions() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> allPermissions = permissionRegistry.allPermissions().stream()
                .map(p -> permissionRegistry.resolveName(p).orElse("UNKNOWN"))
                .distinct()
                .sorted()
                .toList();
        
        response.put("permissions", allPermissions);
        response.put("description", Map.of(
                "READ", "View the resource",
                "WRITE", "Modify the resource",
                "CREATE", "Create child resources",
                "DELETE", "Delete the resource",
                "ADMINISTRATION", "Full control including permission management",
                "SHARE", "Share the resource with others",
                "APPROVE", "Approve changes to the resource"
        ));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/custom-demo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> demonstrateCustomPermissions(
            @RequestParam String resourceType,
            @RequestParam Long resourceId) {
        
        log.info("Demonstrating custom permissions for {} {}", resourceType, resourceId);

        try {
            Class<?> domainClass = resolveDomainClass(resourceType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceType", resourceType);
            response.put("resourceId", resourceId);
            response.put("customPermissions", List.of("SHARE", "APPROVE"));
            response.put("description", Map.of(
                    "SHARE", "Custom permission allowing users to share this resource with others",
                    "APPROVE", "Custom permission for approval workflows"
            ));
            response.put("usage", Map.of(
                    "SHARE", "Grant SHARE permission to allow users to add collaborators",
                    "APPROVE", "Grant APPROVE permission for approval workflows on documents"
            ));
            response.put("example", "Use POST /api/permissions/grant with permission name 'SHARE' or 'APPROVE'");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error demonstrating custom permissions", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private Class<?> resolveDomainClass(String resourceType) {
        return switch (resourceType.toUpperCase()) {
            case "PROJECT" -> Project.class;
            case "DOCUMENT" -> Document.class;
            case "COMMENT" -> Comment.class;
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        };
    }

    private Sid resolveSid(String subjectType, String subjectIdentifier) {
        return switch (subjectType.toUpperCase()) {
            case "USER" -> sidResolver.principalSid(subjectIdentifier);
            case "ROLE" -> sidResolver.roleSid(Role.valueOf(subjectIdentifier.toUpperCase()));
            case "GROUP" -> sidResolver.groupSid(Group.valueOf(subjectIdentifier.toUpperCase()));
            default -> throw new IllegalArgumentException("Unknown subject type: " + subjectType);
        };
    }
}
