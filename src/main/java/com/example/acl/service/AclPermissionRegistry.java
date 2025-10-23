package com.example.acl.service;

import com.example.acl.security.CustomAclPermission;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AclPermissionRegistry implements PermissionFactory {

    private final Map<Integer, Permission> permissionsByMask = new ConcurrentHashMap<>();
    private final Map<String, Permission> permissionsByName = new ConcurrentHashMap<>();
    private final Map<Integer, String> namesByMask = new ConcurrentHashMap<>();
    private final List<Permission> ownerDefaults;

    public AclPermissionRegistry() {
        register("READ", BasePermission.READ);
        register("WRITE", BasePermission.WRITE);
        register("CREATE", BasePermission.CREATE);
        register("DELETE", BasePermission.DELETE);
        register("ADMINISTRATION", BasePermission.ADMINISTRATION);
        register("SHARE", CustomAclPermission.SHARE);
        register("APPROVE", CustomAclPermission.APPROVE);
        ownerDefaults = List.of(
                BasePermission.ADMINISTRATION,
                BasePermission.READ,
                BasePermission.WRITE,
                BasePermission.DELETE,
                CustomAclPermission.SHARE
        );
    }

    private void register(String name, Permission permission) {
        String key = name.toUpperCase(Locale.ROOT);
        permissionsByName.put(key, permission);
        permissionsByMask.put(permission.getMask(), permission);
        namesByMask.put(permission.getMask(), key);
    }

    @Override
    public Permission buildFromMask(int mask) {
        Permission permission = permissionsByMask.get(mask);
        if (permission == null) {
            throw new IllegalArgumentException("Unknown permission mask: " + mask);
        }
        return permission;
    }

    @Override
    public Permission buildFromName(String name) {
        Permission permission = permissionsByName.get(name.toUpperCase(Locale.ROOT));
        if (permission == null) {
            throw new IllegalArgumentException("Unknown permission name: " + name);
        }
        return permission;
    }

    @Override
    public List<Permission> buildFromNames(List<String> names) {
        return names.stream()
                .map(this::buildFromName)
                .collect(Collectors.toList());
    }

    public Optional<String> resolveName(Permission permission) {
        return Optional.ofNullable(namesByMask.get(permission.getMask()));
    }

    public List<Permission> resolvePermissions(Collection<String> names) {
        return names.stream().map(this::buildFromName).collect(Collectors.toList());
    }

    public List<String> toNames(Collection<Permission> permissions) {
        return permissions.stream()
                .map(this::resolveName)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    public List<Permission> ownerDefaults() {
        return Collections.unmodifiableList(ownerDefaults);
    }

    public Collection<Permission> allPermissions() {
        return Collections.unmodifiableCollection(permissionsByMask.values());
    }
}
