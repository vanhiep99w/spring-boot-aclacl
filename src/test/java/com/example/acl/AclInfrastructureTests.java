package com.example.acl;

import com.example.acl.domain.Comment;
import com.example.acl.repository.CommentRepository;
import com.example.acl.security.CustomAclPermission;
import com.example.acl.service.AclAuditLogStore;
import com.example.acl.service.AclPermissionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AclInfrastructureTests {

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AclPermissionRegistry permissionRegistry;

    @Autowired
    private AclAuditLogStore auditLogStore;

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
    void testPrincipalSidsCreated() {
        Integer sidCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_sid WHERE principal = true", Integer.class);
        assertThat(sidCount).isEqualTo(5);
    }

    @Test
    void testAclClassesCreated() {
        List<String> classNames = jdbcTemplate.query("SELECT class FROM acl_class", (rs, rowNum) -> rs.getString(1));
        assertThat(classNames)
                .contains("com.example.acl.domain.Project", "com.example.acl.domain.Document", "com.example.acl.domain.Comment");
        assertThat(classNames).hasSize(3);
    }

    @Test
    void testAclObjectIdentitiesCreated() {
        Integer identityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_object_identity", Integer.class);
        assertThat(identityCount).isEqualTo(14);
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
                "SELECT COUNT(*) FROM acl_entry WHERE mask IN (1, 2, 4, 8, 16, 32, 64)",
                Integer.class);
        Integer totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acl_entry", Integer.class);

        assertThat(validMaskCount).isEqualTo(totalCount);
    }

    @Test
    void testCommentInheritanceConfigured() {
        Comment comment = commentRepository.findAll().stream().findFirst().orElseThrow();
        MutableAcl commentAcl = (MutableAcl) aclService.readAclById(new ObjectIdentityImpl(Comment.class, comment.getId()));

        assertThat(commentAcl.getParentAcl()).isNotNull();
        assertThat(commentAcl.isEntriesInheriting()).isTrue();
        assertThat(commentAcl.getParentAcl().getObjectIdentity().getIdentifier()).isEqualTo(comment.getDocument().getId());
    }

    @Test
    void testGroupSidsHaveEntries() {
        Integer groupSidCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acl_sid WHERE principal = false AND sid LIKE 'GROUP_%'",
                Integer.class);
        Integer groupAceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acl_entry e JOIN acl_sid s ON e.sid = s.id WHERE s.sid LIKE 'GROUP_%'",
                Integer.class);

        assertThat(groupSidCount).isGreaterThan(0);
        assertThat(groupAceCount).isGreaterThan(0);
    }

    @Test
    void testAuditLogCapturedChanges() {
        assertThat(auditLogStore.findAll()).isNotEmpty();
    }

    @Test
    void testPermissionRegistryResolvesCustomPermissions() {
        assertThat(permissionRegistry.buildFromName("SHARE")).isEqualTo(CustomAclPermission.SHARE);
        assertThat(permissionRegistry.buildFromName("APPROVE")).isEqualTo(CustomAclPermission.APPROVE);
    }
}
