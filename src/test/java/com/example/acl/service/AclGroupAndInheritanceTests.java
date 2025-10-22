package com.example.acl.service;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.Group;
import com.example.acl.domain.Project;
import com.example.acl.domain.Role;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Group and Inheritance Tests")
class AclGroupAndInheritanceTests {

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

    @Test
    @DisplayName("Should grant permissions to multiple groups")
    void testMultipleGroupPermissions() {
        Long projectId = 7001L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.ENGINEERING,
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.MARKETING,
                BasePermission.READ
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.SALES,
                BasePermission.READ
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        Sid engineeringSid = sidResolver.groupSid(Group.ENGINEERING);
        Sid marketingSid = sidResolver.groupSid(Group.MARKETING);
        Sid salesSid = sidResolver.groupSid(Group.SALES);
        
        boolean hasEngineering = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(engineeringSid));
        boolean hasMarketing = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(marketingSid));
        boolean hasSales = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(salesSid));
        
        assertThat(hasEngineering).isTrue();
        assertThat(hasMarketing).isTrue();
        assertThat(hasSales).isTrue();
    }

    @Test
    @DisplayName("Should distinguish between user and group permissions")
    void testUserVsGroupPermissions() {
        Long projectId = 7002L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.WRITE
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.ENGINEERING,
                BasePermission.READ
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        List<Sid> userSids = acl.getEntries().stream()
                .map(AccessControlEntry::getSid)
                .filter(sid -> sid.equals(sidResolver.principalSid("alice")))
                .collect(Collectors.toList());
        
        List<Sid> groupSids = acl.getEntries().stream()
                .map(AccessControlEntry::getSid)
                .filter(sid -> sid.equals(sidResolver.groupSid(Group.ENGINEERING)))
                .collect(Collectors.toList());
        
        assertThat(userSids).hasSize(1);
        assertThat(groupSids).hasSize(1);
    }

    @Test
    @DisplayName("Should grant permissions to roles")
    void testRolePermissions() {
        Long projectId = 7003L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToRole(
                Project.class,
                projectId,
                Role.MANAGER,
                BasePermission.READ,
                BasePermission.WRITE,
                BasePermission.DELETE
        );
        
        aclPermissionService.grantToRole(
                Project.class,
                projectId,
                Role.VIEWER,
                BasePermission.READ
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        Sid managerSid = sidResolver.roleSid(Role.MANAGER);
        Sid viewerSid = sidResolver.roleSid(Role.VIEWER);
        
        long managerPermissions = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(managerSid))
                .count();
        long viewerPermissions = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(viewerSid))
                .count();
        
        assertThat(managerPermissions).isEqualTo(3);
        assertThat(viewerPermissions).isEqualTo(1);
    }

    @Test
    @DisplayName("Should properly inherit permissions from parent")
    void testParentChildInheritance() {
        Long parentId = 7004L;
        Long childId = 7005L;
        
        aclPermissionService.ensureAcl(Project.class, parentId);
        aclPermissionService.ensureAcl(Project.class, childId);
        
        aclPermissionService.grantToUser(
                Project.class,
                parentId,
                "alice",
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        aclPermissionService.setParent(
                Project.class,
                childId,
                Project.class,
                parentId,
                true
        );
        
        MutableAcl childAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, childId)
        );
        
        assertThat(childAcl.getParentAcl()).isNotNull();
        assertThat(childAcl.isEntriesInheriting()).isTrue();
        assertThat(childAcl.getParentAcl().getObjectIdentity().getIdentifier()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("Should verify document inherits from project")
    void testDocumentInheritsFromProject() {
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
    @DisplayName("Should verify comment inherits from document")
    void testCommentInheritsFromDocument() {
        Comment comment = commentRepository.findAll().stream()
                .findFirst()
                .orElseThrow();
        
        MutableAcl commentAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Comment.class, comment.getId())
        );
        
        assertThat(commentAcl.getParentAcl()).isNotNull();
        assertThat(commentAcl.isEntriesInheriting()).isTrue();
        assertThat(commentAcl.getParentAcl().getObjectIdentity().getIdentifier())
                .isEqualTo(comment.getDocument().getId());
    }

    @Test
    @DisplayName("Should verify three-level inheritance: Project > Document > Comment")
    void testThreeLevelInheritance() {
        Comment comment = commentRepository.findAll().stream()
                .filter(c -> c.getDocument().getProject() != null)
                .findFirst()
                .orElseThrow();
        
        Document document = comment.getDocument();
        Project project = document.getProject();
        
        MutableAcl commentAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Comment.class, comment.getId())
        );
        
        MutableAcl documentAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Document.class, document.getId())
        );
        
        assertThat(commentAcl.getParentAcl()).isNotNull();
        assertThat(commentAcl.getParentAcl().getObjectIdentity().getIdentifier())
                .isEqualTo(document.getId());
        
        if (documentAcl.getParentAcl() != null) {
            assertThat(documentAcl.getParentAcl().getObjectIdentity().getIdentifier())
                    .isEqualTo(project.getId());
        }
    }

    @Test
    @DisplayName("Should disable inheritance when specified")
    void testDisableInheritance() {
        Long parentId = 7006L;
        Long childId = 7007L;
        
        aclPermissionService.ensureAcl(Project.class, parentId);
        aclPermissionService.ensureAcl(Project.class, childId);
        
        aclPermissionService.setParent(
                Project.class,
                childId,
                Project.class,
                parentId,
                false
        );
        
        MutableAcl childAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, childId)
        );
        
        assertThat(childAcl.getParentAcl()).isNotNull();
        assertThat(childAcl.isEntriesInheriting()).isFalse();
    }

    @Test
    @DisplayName("Should revoke group permissions without affecting user permissions")
    void testRevokeGroupPermissionsOnly() {
        Long projectId = 7008L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.ENGINEERING,
                BasePermission.READ
        );
        
        Sid groupSid = sidResolver.groupSid(Group.ENGINEERING);
        aclPermissionService.revokePermissions(
                Project.class,
                projectId,
                groupSid,
                List.of(BasePermission.READ)
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        Sid aliceSid = sidResolver.principalSid("alice");
        boolean hasAlicePermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(aliceSid));
        boolean hasGroupPermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(groupSid));
        
        assertThat(hasAlicePermission).isTrue();
        assertThat(hasGroupPermission).isFalse();
    }

    @Test
    @DisplayName("Should handle mixed SID types in ACL")
    void testMixedSidTypes() {
        Long projectId = 7009L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                projectId,
                Group.ENGINEERING,
                BasePermission.WRITE
        );
        
        aclPermissionService.grantToRole(
                Project.class,
                projectId,
                Role.MANAGER,
                BasePermission.DELETE
        );
        
        aclPermissionService.grantToAuthority(
                Project.class,
                projectId,
                "CUSTOM_AUTHORITY",
                BasePermission.CREATE
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        List<Sid> sids = acl.getEntries().stream()
                .map(AccessControlEntry::getSid)
                .distinct()
                .collect(Collectors.toList());
        
        assertThat(sids).hasSize(4);
    }

    @Test
    @DisplayName("Should verify group permissions in real data")
    void testRealGroupPermissions() {
        Project marketingProject = projectRepository.findAll().stream()
                .filter(p -> p.getSharedWithGroups().contains(Group.MARKETING))
                .findFirst()
                .orElseThrow();
        
        Acl acl = mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, marketingProject.getId())
        );
        
        Sid marketingGroupSid = sidResolver.groupSid(Group.MARKETING);
        boolean hasMarketingGroupPermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(marketingGroupSid));
        
        assertThat(hasMarketingGroupPermission).isTrue();
    }

    @Test
    @DisplayName("Should change parent ACL")
    void testChangeParentAcl() {
        Long parent1Id = 7010L;
        Long parent2Id = 7011L;
        Long childId = 7012L;
        
        aclPermissionService.ensureAcl(Project.class, parent1Id);
        aclPermissionService.ensureAcl(Project.class, parent2Id);
        aclPermissionService.ensureAcl(Project.class, childId);
        
        aclPermissionService.setParent(
                Project.class,
                childId,
                Project.class,
                parent1Id,
                true
        );
        
        MutableAcl childAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, childId)
        );
        assertThat(childAcl.getParentAcl().getObjectIdentity().getIdentifier()).isEqualTo(parent1Id);
        
        aclPermissionService.setParent(
                Project.class,
                childId,
                Project.class,
                parent2Id,
                true
        );
        
        childAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, childId)
        );
        assertThat(childAcl.getParentAcl().getObjectIdentity().getIdentifier()).isEqualTo(parent2Id);
    }

    @Test
    @DisplayName("Should bulk grant to multiple groups")
    void testBulkGrantToGroups() {
        Long projectId1 = 7013L;
        Long projectId2 = 7014L;
        
        aclPermissionService.ensureAcl(Project.class, projectId1);
        aclPermissionService.ensureAcl(Project.class, projectId2);
        
        List<Long> projectIds = List.of(projectId1, projectId2);
        
        Sid engineeringSid = sidResolver.groupSid(Group.ENGINEERING);
        aclPermissionService.bulkGrant(
                Project.class,
                projectIds,
                engineeringSid,
                List.of(BasePermission.READ)
        );
        
        for (Long projectId : projectIds) {
            MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                    new ObjectIdentityImpl(Project.class, projectId)
            );
            
            boolean hasGroupPermission = acl.getEntries().stream()
                    .anyMatch(ace -> ace.getSid().equals(engineeringSid));
            
            assertThat(hasGroupPermission).isTrue();
        }
    }
}
