package com.example.acl.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessibleResourcesResponse {

    private String subject;
    private String resourceType;
    private List<ResourcePermissionInfo> resources;
    private int totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourcePermissionInfo {
        private Long resourceId;
        private String resourceName;
        private List<String> permissions;
        private String accessSource;
    }
}
