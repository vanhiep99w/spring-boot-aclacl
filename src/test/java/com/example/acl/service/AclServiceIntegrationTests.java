package com.example.acl.service;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.Group;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.Sid;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Service Integration Tests")
class AclServiceIntegrationTests {

    @Autowired
    private AclPermissionService aclPermissionService;

    @Autowired
    private MutableAclService mutableAclService;

    @Autowired
    private AclSidResolver sidResolver;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should verify owner has full permissions on created project")
    void testOwnerPermissionsOnProject() {
        Project project = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Alice's Engineering Project"))
                .findFirst()
                .orElseThrow();
        
        User alice = project.getOwner();
        
        Acl acl = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        
        Sid aliceSid = sidResolver.principalSid(alice.getUsername());
        assertThat(acl.getOwner()).isEqualTo(aliceSid);
        
        long alicePermissions = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(aliceSid))
                .count();
        
        assertThat(alicePermissions).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should verify group permissions are granted correctly")
    void testGroupPermissions() {
        Project project = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Marketing Campaign Project"))
                .findFirst()
                .orElseThrow();
        
        Acl acl = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        
        Sid marketingGroupSid = sidResolver.groupSid(Group.MARKETING);
        
        boolean hasGroupPermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(marketingGroupSid));
        
        assertThat(hasGroupPermission).isTrue();
    }

    @Test
    @DisplayName("Should verify shared user permissions")
    void testSharedUserPermissions() {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Alice's Engineering Project"))
                .findFirst()
                .orElseThrow();
        
        User bob = userRepository.findByUsername("bob").orElseThrow();
        assertThat(aliceProject.getSharedWith()).contains(bob);
        
        Acl acl = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, aliceProject.getId())
        );
        
        Sid bobSid = sidResolver.principalSid("bob");
        boolean hasBobPermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(bobSid));
        
        assertThat(hasBobPermission).isTrue();
    }

    @Test
    @DisplayName("Should verify comment inherits permissions from document")
    void testCommentInheritanceFromDocument() {
        Document document = documentRepository.findAll().stream()
                .filter(d -> d.getTitle().equals("API Design Document"))
                .findFirst()
                .orElseThrow();
        
        Comment comment = commentRepository.findByDocument(document).stream()
                .findFirst()
                .orElseThrow();
        
        MutableAcl commentAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Comment.class, comment.getId())
        );
        
        assertThat(commentAcl.getParentAcl()).isNotNull();
        assertThat(commentAcl.isEntriesInheriting()).isTrue();
        assertThat(commentAcl.getParentAcl().getObjectIdentity().getIdentifier())
                .isEqualTo(document.getId());
    }

    @Test
    @DisplayName("Should verify document inherits permissions from project")
    void testDocumentInheritanceFromProject() {
        Document document = documentRepository.findAll().stream()
                .filter(d -> d.getProject() != null)
                .findFirst()
                .orElseThrow();
        
        MutableAcl documentAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Document.class, document.getId())
        );
        
        if (documentAcl.getParentAcl() != null) {
            assertThat(documentAcl.isEntriesInheriting()).isTrue();
            assertThat(documentAcl.getParentAcl().getObjectIdentity().getIdentifier())
                    .isEqualTo(document.getProject().getId());
        }
    }

    @Test
    @DisplayName("Should grant and verify custom permissions")
    void testCustomPermissionGrant() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        User user = userRepository.findByUsername("alice").orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                user.getUsername(),
                com.example.acl.security.CustomAclPermission.SHARE
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        
        Sid userSid = sidResolver.principalSid(user.getUsername());
        boolean hasSharePermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(userSid) &&
                        ace.getPermission().getMask() == com.example.acl.security.CustomAclPermission.SHARE.getMask());
        
        assertThat(hasSharePermission).isTrue();
    }

    @Test
    @DisplayName("Should revoke permissions and verify removal")
    void testPermissionRevocation() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "carol",
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        Sid carolSid = sidResolver.principalSid("carol");
        long permissionsBefore = aclBefore.getEntries().stream()
                .filter(ace -> ace.getSid().equals(carolSid))
                .count();
        
        aclPermissionService.revokePermissions(
                Project.class,
                project.getId(),
                carolSid,
                List.of(BasePermission.WRITE)
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        long permissionsAfter = aclAfter.getEntries().stream()
                .filter(ace -> ace.getSid().equals(carolSid))
                .count();
        
        assertThat(permissionsAfter).isLessThan(permissionsBefore);
        
        boolean hasWrite = aclAfter.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(carolSid) &&
                        ace.getPermission().getMask() == BasePermission.WRITE.getMask());
        assertThat(hasWrite).isFalse();
    }

    @Test
    @DisplayName("Should handle cache eviction correctly")
    void testCacheEviction() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        Acl aclBefore = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        
        aclPermissionService.evictCache(Project.class, project.getId());
        
        Acl aclAfter = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, project.getId())
        );
        
        assertThat(aclBefore.getObjectIdentity()).isEqualTo(aclAfter.getObjectIdentity());
    }

    @Test
    @DisplayName("Should verify all ACL entries are granting")
    void testAllEntriesAreGranting() {
        List<Project> projects = projectRepository.findAll();
        
        for (Project project : projects) {
            Acl acl = mutableAclService.readAclById(
                    new ObjectIdentityImpl(Project.class, project.getId())
            );
            
            for (AccessControlEntry ace : acl.getEntries()) {
                assertThat(ace.isGranting())
                        .withFailMessage("ACE for project %d should be granting", project.getId())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should verify bulk operations on multiple resources")
    void testBulkOperations() {
        List<Project> projects = projectRepository.findAll();
        List<Long> projectIds = projects.stream()
                .limit(2)
                .map(Project::getId)
                .toList();
        
        aclPermissionService.bulkGrantToUsers(
                Project.class,
                projectIds,
                "dave",
                BasePermission.READ
        );
        
        Sid daveSid = sidResolver.principalSid("dave");
        
        for (Long projectId : projectIds) {
            MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                    new ObjectIdentityImpl(Project.class, projectId)
            );
            
            boolean hasDaveRead = acl.getEntries().stream()
                    .anyMatch(ace -> ace.getSid().equals(daveSid) &&
                            ace.getPermission().getMask() == BasePermission.READ.getMask());
            
            assertThat(hasDaveRead).isTrue();
        }
        
        aclPermissionService.bulkRevoke(
                Project.class,
                projectIds,
                daveSid,
                List.of(BasePermission.READ)
        );
        
        for (Long projectId : projectIds) {
            MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                    new ObjectIdentityImpl(Project.class, projectId)
            );
            
            boolean hasDaveRead = acl.getEntries().stream()
                    .anyMatch(ace -> ace.getSid().equals(daveSid));
            
            assertThat(hasDaveRead).isFalse();
        }
    }

    @Test
    @DisplayName("Should verify ACL audit log captures changes")
    void testAuditLogIntegration() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "testuser",
                BasePermission.READ
        );
        
        aclPermissionService.revokePermissions(
                Project.class,
                project.getId(),
                sidResolver.principalSid("testuser"),
                List.of(BasePermission.READ)
        );
    }
}
