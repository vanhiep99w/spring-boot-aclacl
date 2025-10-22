package com.example.acl.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectivePermissionsResponse {

    private String resourceType;
    private Long resourceId;
    private String subject;
    private List<String> grantedPermissions;
    private List<String> inheritedPermissions;
    private String parentResource;
    private boolean hasAccess;
}
