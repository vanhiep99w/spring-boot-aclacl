package com.example.acl.service;

import com.example.acl.domain.Group;
import com.example.acl.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.sid.SidRetrievalStrategy;
import org.springframework.security.acls.sid.SidRetrievalStrategyImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AclPermissionService {

    private static final String SYSTEM_ACTOR = "system";

    private final MutableAclService aclService;
    private final AclCache aclCache;
    private final AclPermissionRegistry permissionRegistry;
    private final AclSidResolver sidResolver;
    private final AclAuditService auditService;
    private final SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    @Transactional
    public MutableAcl ensureAcl(Class<?> domainClass, Serializable identifier) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, identifier);
        try {
            return (MutableAcl) aclService.readAclById(oid);
        } catch (NotFoundException ex) {
            MutableAcl acl = aclService.createAcl(oid);
            auditService.publishChange(AclAuditOperation.CREATE, domainClass, identifier, null, Collections.emptyList(), currentActor());
            log.debug("Created ACL for {} with id {}", domainClass.getSimpleName(), identifier);
            return acl;
        }
    }

    @Transactional
    public void applyOwnership(Class<?> domainClass, Serializable identifier, String username) {
        MutableAcl acl = ensureAcl(domainClass, identifier);
        PrincipalSid ownerSid = sidResolver.principalSid(username);
        List<Permission> defaults = permissionRegistry.ownerDefaults();
        boolean changed = false;
        if (acl.getOwner() == null || !acl.getOwner().equals(ownerSid)) {
            acl.setOwner(ownerSid);
            changed = true;
        }
        changed |= addPermissionsIfMissing(acl, ownerSid, defaults);
        if (changed) {
            updateAcl(acl);
            auditService.publishChange(
                    AclAuditOperation.OWNERSHIP,
                    domainClass,
                    identifier,
                    ownerSid,
                    defaults,
                    currentActor()
            );
        }
    }

    @Transactional
    public void grantToUser(Class<?> domainClass, Serializable identifier, String username, Permission... permissions) {
        grantPermissions(domainClass, identifier, sidResolver.principalSid(username), Arrays.asList(permissions));
    }

    @Transactional
    public void grantToGroup(Class<?> domainClass, Serializable identifier, Group group, Permission... permissions) {
        grantPermissions(domainClass, identifier, sidResolver.groupSid(group), Arrays.asList(permissions));
    }

    @Transactional
    public void grantToRole(Class<?> domainClass, Serializable identifier, Role role, Permission... permissions) {
        grantPermissions(domainClass, identifier, sidResolver.roleSid(role), Arrays.asList(permissions));
    }

    @Transactional
    public void grantToAuthority(Class<?> domainClass, Serializable identifier, String authority, Permission... permissions) {
        grantPermissions(domainClass, identifier, sidResolver.authoritySid(authority), Arrays.asList(permissions));
    }

    @Transactional
    public void grantPermissions(Class<?> domainClass, Serializable identifier, Sid sid, Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        MutableAcl acl = ensureAcl(domainClass, identifier);
        boolean changed = addPermissionsIfMissing(acl, sid, permissions);
        if (changed) {
            updateAcl(acl);
            auditService.publishChange(
                    AclAuditOperation.GRANT,
                    domainClass,
                    identifier,
                    sid,
                    new ArrayList<>(permissions),
                    currentActor()
            );
        }
    }

    @Transactional
    public void bulkGrant(Class<?> domainClass, Collection<? extends Serializable> identifiers, Sid sid, Collection<Permission> permissions) {
        if (identifiers == null || identifiers.isEmpty()) {
            return;
        }
        for (Serializable identifier : identifiers) {
            grantPermissions(domainClass, identifier, sid, permissions);
        }
    }

    @Transactional
    public void bulkGrantToUsers(Class<?> domainClass, Collection<? extends Serializable> identifiers, String username, Permission... permissions) {
        bulkGrant(domainClass, identifiers, sidResolver.principalSid(username), Arrays.asList(permissions));
    }

    @Transactional
    public void revokePermissions(Class<?> domainClass, Serializable identifier, Sid sid, Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        MutableAcl acl = ensureAcl(domainClass, identifier);
        List<Integer> masks = permissions.stream().map(Permission::getMask).toList();
        List<AccessControlEntry> entries = new ArrayList<>(acl.getEntries());
        boolean changed = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            AccessControlEntry entry = entries.get(i);
            if (entry.getSid().equals(sid) && masks.contains(entry.getPermission().getMask())) {
                acl.deleteAce(i);
                changed = true;
            }
        }
        if (changed) {
            updateAcl(acl);
            auditService.publishChange(
                    AclAuditOperation.REVOKE,
                    domainClass,
                    identifier,
                    sid,
                    new ArrayList<>(permissions),
                    currentActor()
            );
        }
    }

    @Transactional
    public void bulkRevoke(Class<?> domainClass, Collection<? extends Serializable> identifiers, Sid sid, Collection<Permission> permissions) {
        if (identifiers == null || identifiers.isEmpty()) {
            return;
        }
        for (Serializable identifier : identifiers) {
            revokePermissions(domainClass, identifier, sid, permissions);
        }
    }

    @Transactional
    public void revokeAllForSid(Class<?> domainClass, Serializable identifier, Sid sid) {
        MutableAcl acl = ensureAcl(domainClass, identifier);
        List<AccessControlEntry> entries = new ArrayList<>(acl.getEntries());
        boolean changed = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            AccessControlEntry entry = entries.get(i);
            if (entry.getSid().equals(sid)) {
                acl.deleteAce(i);
                changed = true;
            }
        }
        if (changed) {
            updateAcl(acl);
        }
    }

    @Transactional
    public void setParent(Class<?> childClass, Serializable childId, Class<?> parentClass, Serializable parentId, boolean entriesInheriting) {
        MutableAcl childAcl = ensureAcl(childClass, childId);
        MutableAcl parentAcl = ensureAcl(parentClass, parentId);
        boolean changed = false;
        if (!Objects.equals(childAcl.getParentAcl(), parentAcl)) {
            childAcl.setParent(parentAcl);
            changed = true;
        }
        if (childAcl.isEntriesInheriting() != entriesInheriting) {
            childAcl.setEntriesInheriting(entriesInheriting);
            changed = true;
        }
        if (changed) {
            updateAcl(childAcl);
            auditService.publishChange(
                    AclAuditOperation.INHERITANCE,
                    childClass,
                    childId,
                    null,
                    Collections.emptyList(),
                    currentActor()
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Authentication authentication, Class<?> domainClass, Serializable identifier, Permission... permissions) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, identifier);
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
        try {
            Acl acl = aclService.readAclById(oid, sids);
            return acl.isGranted(Arrays.asList(permissions), sids, false);
        } catch (NotFoundException ex) {
            return false;
        }
    }

    public void evictCache(Class<?> domainClass, Serializable identifier) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, identifier);
        aclCache.evictFromCache(oid);
    }

    private boolean addPermissionsIfMissing(MutableAcl acl, Sid sid, Collection<Permission> permissions) {
        boolean changed = false;
        for (Permission permission : permissions) {
            if (!isPermissionGranted(acl, sid, permission)) {
                acl.insertAce(acl.getEntries().size(), permission, sid, true);
                changed = true;
            }
        }
        return changed;
    }

    private boolean isPermissionGranted(MutableAcl acl, Sid sid, Permission permission) {
        for (AccessControlEntry entry : acl.getEntries()) {
            if (entry.getSid().equals(sid) && entry.getPermission().getMask() == permission.getMask() && entry.isGranting()) {
                return true;
            }
        }
        return false;
    }

    private void updateAcl(MutableAcl acl) {
        aclService.updateAcl(acl);
        aclCache.evictFromCache(acl.getObjectIdentity());
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SYSTEM_ACTOR;
        }
        return authentication.getName();
    }
}
