package com.example.acl.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;

@Service
public class AclAuditService {

    private final ApplicationEventPublisher eventPublisher;

    public AclAuditService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishChange(
            AclAuditOperation operation,
            Class<?> domainType,
            Serializable identifier,
            org.springframework.security.acls.model.Sid sid,
            Collection<org.springframework.security.acls.model.Permission> permissions,
            String actor
    ) {
        AclPermissionChangeEvent event = new AclPermissionChangeEvent(
                this,
                operation,
                domainType,
                identifier,
                sid,
                permissions,
                actor
        );
        eventPublisher.publishEvent(event);
    }
}
