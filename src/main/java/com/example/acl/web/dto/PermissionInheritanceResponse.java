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
public class PermissionInheritanceResponse {

    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private boolean hasParent;
    private ParentResourceInfo parent;
    private List<String> directPermissions;
    private List<String> inheritedPermissions;
    private boolean entriesInheriting;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentResourceInfo {
        private String resourceType;
        private Long resourceId;
        private String resourceName;
        private List<String> permissions;
    }
}
