package com.example.acl.service;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.io.Serializable;
import java.util.Collection;

public class AclPermissionChangeEvent extends ApplicationEvent {

    private final AclAuditOperation operation;
    private final Class<?> domainType;
    private final Serializable identifier;
    private final Sid sid;
    private final Collection<Permission> permissions;
    private final String actor;

    public AclPermissionChangeEvent(
            Object source,
            AclAuditOperation operation,
            Class<?> domainType,
            Serializable identifier,
            Sid sid,
            Collection<Permission> permissions,
            String actor
    ) {
        super(source);
        this.operation = operation;
        this.domainType = domainType;
        this.identifier = identifier;
        this.sid = sid;
        this.permissions = permissions;
        this.actor = actor;
    }

    public AclAuditOperation getOperation() {
        return operation;
    }

    public Class<?> getDomainType() {
        return domainType;
    }

    public Serializable getIdentifier() {
        return identifier;
    }

    public Sid getSid() {
        return sid;
    }

    public Collection<Permission> getPermissions() {
        return permissions;
    }

    public String getActor() {
        return actor;
    }
}
