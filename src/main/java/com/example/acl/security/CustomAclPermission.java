package com.example.acl.security;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

public class CustomAclPermission extends BasePermission {

    public static final Permission SHARE = new CustomAclPermission(1 << 5, 'S');
    public static final Permission APPROVE = new CustomAclPermission(1 << 6, 'A');

    private CustomAclPermission(int mask, char code) {
        super(mask, code);
    }
}
