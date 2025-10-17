package com.example.acl.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/acl")
@RequiredArgsConstructor
@Slf4j
public class AclTestController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAclStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Integer sidCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_sid", Integer.class);
            Integer classCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_class", Integer.class);
            Integer objectIdentityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_object_identity", Integer.class);
            Integer entryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_entry", Integer.class);
            
            status.put("aclSids", sidCount);
            status.put("aclClasses", classCount);
            status.put("aclObjectIdentities", objectIdentityCount);
            status.put("aclEntries", entryCount);
            status.put("status", "ACL infrastructure is operational");
            
            log.info("ACL Status: {} sids, {} classes, {} identities, {} entries", 
                    sidCount, classCount, objectIdentityCount, entryCount);
            
        } catch (Exception e) {
            log.error("Error querying ACL tables", e);
            status.put("status", "Error querying ACL tables");
            status.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
}
