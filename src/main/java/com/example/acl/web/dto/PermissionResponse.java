package com.example.acl.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

    private boolean success;
    private String message;
    private String resourceType;
    private Long resourceId;
    private String subject;
}
