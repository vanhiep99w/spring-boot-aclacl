package com.example.acl.service;

import java.time.Instant;
import java.util.List;

public record AclAuditLogEntry(
        Instant timestamp,
        String actor,
        AclAuditOperation operation,
        String domainType,
        String objectId,
        String sid,
        List<String> permissions
) {
}
