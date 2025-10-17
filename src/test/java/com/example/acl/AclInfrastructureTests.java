package com.example.acl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.model.MutableAclService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AclInfrastructureTests {

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertThat(aclService).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    void testAclTablesExist() {
        Integer sidCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_sid", Integer.class);
        Integer classCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_class", Integer.class);
        Integer identityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_object_identity", Integer.class);
        Integer entryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_entry", Integer.class);

        assertThat(sidCount).isNotNull().isGreaterThan(0);
        assertThat(classCount).isNotNull().isGreaterThan(0);
        assertThat(identityCount).isNotNull().isGreaterThan(0);
        assertThat(entryCount).isNotNull().isGreaterThan(0);
    }

    @Test
    void testAclSidsCreated() {
        Integer sidCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_sid", Integer.class);
        assertThat(sidCount).isEqualTo(5);
    }

    @Test
    void testAclClassesCreated() {
        Integer classCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_class", Integer.class);
        assertThat(classCount).isEqualTo(2);
        
        Integer projectClassCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acl_class WHERE class = ?", 
                Integer.class, 
                "com.example.acl.domain.Project");
        assertThat(projectClassCount).isEqualTo(1);

        Integer documentClassCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acl_class WHERE class = ?", 
                Integer.class, 
                "com.example.acl.domain.Document");
        assertThat(documentClassCount).isEqualTo(1);
    }

    @Test
    void testAclObjectIdentitiesCreated() {
        Integer identityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_object_identity", Integer.class);
        assertThat(identityCount).isEqualTo(9);
    }

    @Test
    void testAclEntriesCreated() {
        Integer entryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_entry", Integer.class);
        assertThat(entryCount).isGreaterThan(0);
    }

    @Test
    void testUserSidsExist() {
        String[] expectedUsers = {"admin", "alice", "bob", "carol", "dave"};
        
        for (String username : expectedUsers) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM acl_sid WHERE sid = ? AND principal = true", 
                    Integer.class, 
                    username);
            assertThat(count)
                    .withFailMessage("User SID for '%s' should exist", username)
                    .isEqualTo(1);
        }
    }

    @Test
    void testAclEntriesHaveValidPermissions() {
        Integer validMaskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acl_entry WHERE mask IN (1, 2, 4, 8, 16)", 
                Integer.class);
        Integer totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_entry", Integer.class);
        
        assertThat(validMaskCount).isEqualTo(totalCount);
    }
}
