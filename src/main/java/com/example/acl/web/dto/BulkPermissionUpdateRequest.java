package com.example.acl.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPermissionUpdateRequest {

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    @NotEmpty(message = "At least one resource ID is required")
    private List<Long> resourceIds;

    @NotBlank(message = "Subject type is required (USER, ROLE, or GROUP)")
    private String subjectType;

    @NotBlank(message = "Subject identifier is required")
    private String subjectIdentifier;

    @NotBlank(message = "Operation is required (GRANT or REVOKE)")
    private String operation;

    @NotEmpty(message = "At least one permission is required")
    private List<String> permissions;
}
