package com.example.acl.service;

import com.example.acl.domain.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Negative Path Tests")
class AclNegativePathTests {

    @Autowired
    private AclPermissionService aclPermissionService;

    @Autowired
    private MutableAclService mutableAclService;

    @Autowired
    private AclSidResolver sidResolver;

    @Test
    @DisplayName("Should handle non-existent ACL gracefully in hasPermission")
    void testNonExistentAclInHasPermission() {
        Long nonExistentId = 999999L;
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        boolean hasPermission = aclPermissionService.hasPermission(
                auth,
                Project.class,
                nonExistentId,
                BasePermission.READ
        );
        
        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should throw NotFoundException when reading non-existent ACL directly")
    void testNonExistentAclDirect() {
        Long nonExistentId = 888888L;
        
        assertThatThrownBy(() -> 
                mutableAclService.readAclById(new ObjectIdentityImpl(Project.class, nonExistentId))
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should handle null permission list gracefully")
    void testNullPermissionList() {
        Long projectId = 8001L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        assertThatCode(() -> 
                aclPermissionService.grantPermissions(
                        Project.class,
                        projectId,
                        sidResolver.principalSid("alice"),
                        null
                )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty permission list gracefully")
    void testEmptyPermissionList() {
        Long projectId = 8002L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        assertThatCode(() -> 
                aclPermissionService.grantPermissions(
                        Project.class,
                        projectId,
                        sidResolver.principalSid("alice"),
                        Collections.emptyList()
                )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle revoke on non-existent permission gracefully")
    void testRevokeNonExistentPermission() {
        Long projectId = 8003L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        Sid userSid = sidResolver.principalSid("alice");
        
        assertThatCode(() -> 
                aclPermissionService.revokePermissions(
                        Project.class,
                        projectId,
                        userSid,
                        List.of(BasePermission.WRITE)
                )
        ).doesNotThrowAnyException();
        
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        assertThat(acl).isNotNull();
    }

    @Test
    @DisplayName("Should handle revokeAllForSid when SID has no permissions")
    void testRevokeAllForSidWithNoPermissions() {
        Long projectId = 8004L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        Sid userSid = sidResolver.principalSid("nonexistent");
        
        assertThatCode(() -> 
                aclPermissionService.revokeAllForSid(Project.class, projectId, userSid)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should return false for hasPermission with null authentication")
    void testHasPermissionWithNullAuth() {
        Long projectId = 8005L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        SecurityContextHolder.clearContext();
        
        boolean hasPermission = aclPermissionService.hasPermission(
                null,
                Project.class,
                projectId,
                BasePermission.READ
        );
        
        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should handle bulk operations with empty ID list")
    void testBulkOperationsWithEmptyList() {
        assertThatCode(() -> 
                aclPermissionService.bulkGrant(
                        Project.class,
                        Collections.emptyList(),
                        sidResolver.principalSid("alice"),
                        List.of(BasePermission.READ)
                )
        ).doesNotThrowAnyException();
        
        assertThatCode(() -> 
                aclPermissionService.bulkRevoke(
                        Project.class,
                        Collections.emptyList(),
                        sidResolver.principalSid("alice"),
                        List.of(BasePermission.READ)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle bulk operations with null ID list")
    void testBulkOperationsWithNullList() {
        assertThatCode(() -> 
                aclPermissionService.bulkGrant(
                        Project.class,
                        null,
                        sidResolver.principalSid("alice"),
                        List.of(BasePermission.READ)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should verify user without permission cannot access")
    void testUserWithoutPermissionDenied() {
        Long projectId = 8006L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        Authentication bobAuth = new UsernamePasswordAuthenticationToken(
                "bob",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        boolean hasPermission = aclPermissionService.hasPermission(
                bobAuth,
                Project.class,
                projectId,
                BasePermission.READ
        );
        
        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should verify user with read cannot write")
    void testReadOnlyUserCannotWrite() {
        Long projectId = 8007L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "carol",
                BasePermission.READ
        );
        
        Authentication carolAuth = new UsernamePasswordAuthenticationToken(
                "carol",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        boolean hasRead = aclPermissionService.hasPermission(
                carolAuth,
                Project.class,
                projectId,
                BasePermission.READ
        );
        
        boolean hasWrite = aclPermissionService.hasPermission(
                carolAuth,
                Project.class,
                projectId,
                BasePermission.WRITE
        );
        
        assertThat(hasRead).isTrue();
        assertThat(hasWrite).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple revocations of same permission")
    void testMultipleRevocations() {
        Long projectId = 8008L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "dave",
                BasePermission.READ
        );
        
        Sid daveSid = sidResolver.principalSid("dave");
        
        aclPermissionService.revokePermissions(
                Project.class,
                projectId,
                daveSid,
                List.of(BasePermission.READ)
        );
        
        assertThatCode(() -> 
                aclPermissionService.revokePermissions(
                        Project.class,
                        projectId,
                        daveSid,
                        List.of(BasePermission.READ)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should not grant duplicate permissions on multiple calls")
    void testDuplicatePermissionGrants() {
        Long projectId = 8009L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        int entriesBefore = aclBefore.getEntries().size();
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, projectId)
        );
        
        assertThat(aclAfter.getEntries().size()).isEqualTo(entriesBefore);
    }

    @Test
    @DisplayName("Should handle evict cache on non-cached object")
    void testEvictCacheOnNonCached() {
        Long nonCachedId = 8010L;
        
        assertThatCode(() -> 
                aclPermissionService.evictCache(Project.class, nonCachedId)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle permission check with invalid permission mask")
    void testInvalidPermissionMask() {
        Long projectId = 8011L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        aclPermissionService.grantToUser(
                Project.class,
                projectId,
                "alice",
                BasePermission.READ
        );
        
        Authentication aliceAuth = new UsernamePasswordAuthenticationToken(
                "alice",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        boolean hasDelete = aclPermissionService.hasPermission(
                aliceAuth,
                Project.class,
                projectId,
                BasePermission.DELETE
        );
        
        assertThat(hasDelete).isFalse();
    }

    @Test
    @DisplayName("Should verify permission is not inherited when disabled")
    void testNoInheritanceWhenDisabled() {
        Long parentId = 8012L;
        Long childId = 8013L;
        
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
                false
        );
        
        MutableAcl childAcl = (MutableAcl) mutableAclService.readAclById(
                new ObjectIdentityImpl(Project.class, childId)
        );
        
        assertThat(childAcl.isEntriesInheriting()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty varargs permissions")
    void testEmptyVarargs() {
        Long projectId = 8014L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        assertThatCode(() -> 
                aclPermissionService.grantToUser(
                        Project.class,
                        projectId,
                        "alice"
                )
        ).doesNotThrowAnyException();
    }
}
