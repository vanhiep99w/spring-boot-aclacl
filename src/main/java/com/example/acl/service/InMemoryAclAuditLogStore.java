package com.example.acl.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryAclAuditLogStore implements AclAuditLogStore {

    private final CopyOnWriteArrayList<AclAuditLogEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void save(AclAuditLogEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<AclAuditLogEntry> findAll() {
        return List.copyOf(entries);
    }
}
