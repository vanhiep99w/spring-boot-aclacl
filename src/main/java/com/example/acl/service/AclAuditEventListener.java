package com.example.acl.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
public class AclAuditEventListener {

    private final AclAuditLogStore logStore;
    private final AclPermissionRegistry permissionRegistry;

    public AclAuditEventListener(AclAuditLogStore logStore, AclPermissionRegistry permissionRegistry) {
        this.logStore = logStore;
        this.permissionRegistry = permissionRegistry;
    }

    @EventListener
    public void onPermissionChange(AclPermissionChangeEvent event) {
        List<String> permissionNames = permissionNames(event.getPermissions());
        String sidValue = sidValue(event.getSid());
        AclAuditLogEntry entry = new AclAuditLogEntry(
                Instant.now(),
                event.getActor(),
                event.getOperation(),
                event.getDomainType().getName(),
                String.valueOf(event.getIdentifier()),
                sidValue,
                permissionNames
        );
        logStore.save(entry);
    }

    private List<String> permissionNames(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return permissionRegistry.toNames(permissions);
    }

    private String sidValue(Sid sid) {
        if (sid instanceof PrincipalSid principalSid) {
            return principalSid.getPrincipal();
        }
        if (sid instanceof GrantedAuthoritySid authoritySid) {
            return authoritySid.getGrantedAuthority();
        }
        return sid != null ? sid.toString() : null;
    }
}
