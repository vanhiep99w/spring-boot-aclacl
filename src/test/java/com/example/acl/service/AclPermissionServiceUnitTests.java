package com.example.acl.service;

import com.example.acl.domain.Group;
import com.example.acl.domain.Project;
import com.example.acl.domain.Role;
import com.example.acl.security.CustomAclPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Permission Service Unit Tests")
class AclPermissionServiceUnitTests {

    @Autowired
    private AclPermissionService aclPermissionService;

    @Autowired
    private MutableAclService mutableAclService;

    @Autowired
    private AclSidResolver sidResolver;

    @Autowired
    private AclCache aclCache;

    private static final Long TEST_PROJECT_ID = 999L;
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                TEST_USERNAME,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should ensure ACL exists for new object")
    void testEnsureAcl() {
        MutableAcl acl = aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        
        assertThat(acl).isNotNull();
        assertThat(acl.getObjectIdentity()).isNotNull();
        assertThat(acl.getObjectIdentity().getIdentifier()).isEqualTo(TEST_PROJECT_ID);
    }

    @Test
    @DisplayName("Should apply ownership with default permissions")
    void testApplyOwnership() {
        aclPermissionService.applyOwnership(Project.class, TEST_PROJECT_ID, TEST_USERNAME);
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        assertThat(acl.getOwner()).isEqualTo(sidResolver.principalSid(TEST_USERNAME));
        assertThat(acl.getEntries()).isNotEmpty();
        
        List<Permission> grantedPermissions = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(sidResolver.principalSid(TEST_USERNAME)))
                .map(AccessControlEntry::getPermission)
                .toList();
        
        assertThat(grantedPermissions).isNotEmpty();
    }

    @Test
    @DisplayName("Should grant permissions to user")
    void testGrantToUser() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "alice",
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid aliceSid = sidResolver.principalSid("alice");
        List<Integer> grantedMasks = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(aliceSid))
                .map(ace -> ace.getPermission().getMask())
                .toList();
        
        assertThat(grantedMasks).contains(
                BasePermission.READ.getMask(),
                BasePermission.WRITE.getMask()
        );
    }

    @Test
    @DisplayName("Should grant permissions to group")
    void testGrantToGroup() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToGroup(
                Project.class,
                TEST_PROJECT_ID,
                Group.ENGINEERING,
                BasePermission.READ
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid groupSid = sidResolver.groupSid(Group.ENGINEERING);
        boolean hasPermission = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(groupSid) &&
                        ace.getPermission().getMask() == BasePermission.READ.getMask());
        
        assertThat(hasPermission).isTrue();
    }

    @Test
    @DisplayName("Should grant permissions to role")
    void testGrantToRole() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToRole(
                Project.class,
                TEST_PROJECT_ID,
                Role.MANAGER,
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid roleSid = sidResolver.roleSid(Role.MANAGER);
        long matchingEntries = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(roleSid))
                .count();
        
        assertThat(matchingEntries).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should grant custom permissions")
    void testGrantCustomPermissions() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "bob",
                CustomAclPermission.SHARE,
                CustomAclPermission.APPROVE
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid bobSid = sidResolver.principalSid("bob");
        List<Integer> grantedMasks = acl.getEntries().stream()
                .filter(ace -> ace.getSid().equals(bobSid))
                .map(ace -> ace.getPermission().getMask())
                .toList();
        
        assertThat(grantedMasks).contains(
                CustomAclPermission.SHARE.getMask(),
                CustomAclPermission.APPROVE.getMask()
        );
    }

    @Test
    @DisplayName("Should revoke permissions from user")
    void testRevokePermissions() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "carol",
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        int entriesBeforeRevoke = aclBefore.getEntries().size();
        
        aclPermissionService.revokePermissions(
                Project.class,
                TEST_PROJECT_ID,
                sidResolver.principalSid("carol"),
                Arrays.asList(BasePermission.READ)
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid carolSid = sidResolver.principalSid("carol");
        boolean hasRead = aclAfter.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(carolSid) &&
                        ace.getPermission().getMask() == BasePermission.READ.getMask());
        
        assertThat(hasRead).isFalse();
        assertThat(aclAfter.getEntries().size()).isLessThan(entriesBeforeRevoke);
    }

    @Test
    @DisplayName("Should revoke all permissions for a SID")
    void testRevokeAllForSid() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "dave",
                BasePermission.READ,
                BasePermission.WRITE,
                BasePermission.DELETE
        );
        
        Sid daveSid = sidResolver.principalSid("dave");
        aclPermissionService.revokeAllForSid(Project.class, TEST_PROJECT_ID, daveSid);
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        boolean hasDavePermissions = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(daveSid));
        
        assertThat(hasDavePermissions).isFalse();
    }

    @Test
    @DisplayName("Should perform bulk grant operations")
    void testBulkGrant() {
        Long projectId1 = 1001L;
        Long projectId2 = 1002L;
        
        List<Long> projectIds = Arrays.asList(projectId1, projectId2);
        
        aclPermissionService.bulkGrantToUsers(
                Project.class,
                projectIds,
                "alice",
                BasePermission.READ
        );
        
        MutableAcl acl1 = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId1)
        );
        MutableAcl acl2 = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId2)
        );
        
        Sid aliceSid = sidResolver.principalSid("alice");
        
        boolean acl1HasPermission = acl1.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(aliceSid) &&
                        ace.getPermission().getMask() == BasePermission.READ.getMask());
        boolean acl2HasPermission = acl2.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(aliceSid) &&
                        ace.getPermission().getMask() == BasePermission.READ.getMask());
        
        assertThat(acl1HasPermission).isTrue();
        assertThat(acl2HasPermission).isTrue();
    }

    @Test
    @DisplayName("Should perform bulk revoke operations")
    void testBulkRevoke() {
        Long projectId1 = 2001L;
        Long projectId2 = 2002L;
        
        aclPermissionService.ensureAcl(Project.class, projectId1);
        aclPermissionService.ensureAcl(Project.class, projectId2);
        
        aclPermissionService.grantToUser(Project.class, projectId1, "bob", BasePermission.READ);
        aclPermissionService.grantToUser(Project.class, projectId2, "bob", BasePermission.READ);
        
        List<Long> projectIds = Arrays.asList(projectId1, projectId2);
        
        aclPermissionService.bulkRevoke(
                Project.class,
                projectIds,
                sidResolver.principalSid("bob"),
                Arrays.asList(BasePermission.READ)
        );
        
        MutableAcl acl1 = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId1)
        );
        MutableAcl acl2 = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId2)
        );
        
        Sid bobSid = sidResolver.principalSid("bob");
        
        boolean acl1HasPermission = acl1.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(bobSid));
        boolean acl2HasPermission = acl2.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(bobSid));
        
        assertThat(acl1HasPermission).isFalse();
        assertThat(acl2HasPermission).isFalse();
    }

    @Test
    @DisplayName("Should set parent ACL for inheritance")
    void testSetParent() {
        Long parentId = 3001L;
        Long childId = 3002L;
        
        aclPermissionService.ensureAcl(Project.class, parentId);
        aclPermissionService.ensureAcl(Project.class, childId);
        
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
    @DisplayName("Should check hasPermission correctly")
    void testHasPermission() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                TEST_USERNAME,
                BasePermission.READ
        );
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        boolean hasReadPermission = aclPermissionService.hasPermission(
                auth,
                Project.class,
                TEST_PROJECT_ID,
                BasePermission.READ
        );
        
        boolean hasWritePermission = aclPermissionService.hasPermission(
                auth,
                Project.class,
                TEST_PROJECT_ID,
                BasePermission.WRITE
        );
        
        assertThat(hasReadPermission).isTrue();
        assertThat(hasWritePermission).isFalse();
    }

    @Test
    @DisplayName("Should evict cache for object")
    void testEvictCache() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        
        assertThatCode(() -> aclPermissionService.evictCache(Project.class, TEST_PROJECT_ID))
                .doesNotThrowAnyException();
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        assertThat(acl).isNotNull();
    }

    @Test
    @DisplayName("Should not grant duplicate permissions")
    void testNoDuplicatePermissions() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "alice",
                BasePermission.READ
        );
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        int entriesBefore = aclBefore.getEntries().size();
        
        aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "alice",
                BasePermission.READ
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        assertThat(aclAfter.getEntries().size()).isEqualTo(entriesBefore);
    }

    @Test
    @DisplayName("Should handle multiple groups")
    void testMultipleGroups() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        
        aclPermissionService.grantToGroup(
                Project.class,
                TEST_PROJECT_ID,
                Group.ENGINEERING,
                BasePermission.READ
        );
        
        aclPermissionService.grantToGroup(
                Project.class,
                TEST_PROJECT_ID,
                Group.MARKETING,
                BasePermission.READ
        );
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, TEST_PROJECT_ID)
        );
        
        Sid engineeringSid = sidResolver.groupSid(Group.ENGINEERING);
        Sid marketingSid = sidResolver.groupSid(Group.MARKETING);
        
        boolean hasEngineering = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(engineeringSid));
        boolean hasMarketing = acl.getEntries().stream()
                .anyMatch(ace -> ace.getSid().equals(marketingSid));
        
        assertThat(hasEngineering).isTrue();
        assertThat(hasMarketing).isTrue();
    }

    @Test
    @DisplayName("Should handle empty permission list gracefully")
    void testEmptyPermissionList() {
        aclPermissionService.ensureAcl(Project.class, TEST_PROJECT_ID);
        
        assertThatCode(() -> aclPermissionService.grantToUser(
                Project.class,
                TEST_PROJECT_ID,
                "alice"
        )).doesNotThrowAnyException();
    }
}
