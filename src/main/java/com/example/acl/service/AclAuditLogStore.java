package com.example.acl.service;

import java.util.List;

public interface AclAuditLogStore {

    void save(AclAuditLogEntry entry);

    List<AclAuditLogEntry> findAll();
}
