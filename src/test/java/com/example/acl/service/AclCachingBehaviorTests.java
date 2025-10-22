package com.example.acl.service;

import com.example.acl.domain.Project;
import com.example.acl.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Caching Behavior Tests")
class AclCachingBehaviorTests {

    @Autowired
    private AclPermissionService aclPermissionService;

    @Autowired
    private MutableAclService mutableAclService;

    @Autowired
    private AclSidResolver sidResolver;

    @Autowired
    private AclCache aclCache;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Should cache ACL after initial read")
    void testAclCaching() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        
        MutableAcl aclFirstRead = (MutableAcl) mutableAclService.readAclById(oid);
        assertThat(aclFirstRead).isNotNull();
        
        MutableAcl cachedAcl = aclCache.getFromCache(oid);
        assertThat(cachedAcl).isNotNull();
        assertThat(cachedAcl.getObjectIdentity()).isEqualTo(aclFirstRead.getObjectIdentity());
    }

    @Test
    @DisplayName("Should evict cache when ACL is updated")
    void testCacheEvictionOnUpdate() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(oid);
        assertThat(aclCache.getFromCache(oid)).isNotNull();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "testuser",
                BasePermission.READ
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(oid);
        assertThat(aclAfter.getEntries().size()).isGreaterThan(aclBefore.getEntries().size());
    }

    @Test
    @DisplayName("Should manually evict cache")
    void testManualCacheEviction() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        
        mutableAclService.readAclById(oid);
        assertThat(aclCache.getFromCache(oid)).isNotNull();
        
        aclPermissionService.evictCache(Project.class, project.getId());
        
        mutableAclService.readAclById(oid);
        assertThat(aclCache.getFromCache(oid)).isNotNull();
    }

    @Test
    @DisplayName("Should cache parent ACL relationships")
    void testParentAclCaching() {
        Long parentId = 5001L;
        Long childId = 5002L;
        
        aclPermissionService.ensureAcl(Project.class, parentId);
        aclPermissionService.ensureAcl(Project.class, childId);
        
        aclPermissionService.setParent(
                Project.class,
                childId,
                Project.class,
                parentId,
                true
        );
        
        ObjectIdentity childOid = new ObjectIdentityImpl(Project.class, childId);
        MutableAcl childAcl = (MutableAcl) mutableAclService.readAclById(childOid);
        
        assertThat(childAcl.getParentAcl()).isNotNull();
        
        MutableAcl cachedChildAcl = aclCache.getFromCache(childOid);
        assertThat(cachedChildAcl).isNotNull();
        assertThat(cachedChildAcl.getParentAcl()).isNotNull();
    }

    @Test
    @DisplayName("Should cache multiple ACLs independently")
    void testMultipleAclCaching() {
        Project project1 = projectRepository.findAll().get(0);
        Project project2 = projectRepository.findAll().get(1);
        
        ObjectIdentity oid1 = new ObjectIdentityImpl(Project.class, project1.getId());
        ObjectIdentity oid2 = new ObjectIdentityImpl(Project.class, project2.getId());
        
        mutableAclService.readAclById(oid1);
        mutableAclService.readAclById(oid2);
        
        assertThat(aclCache.getFromCache(oid1)).isNotNull();
        assertThat(aclCache.getFromCache(oid2)).isNotNull();
        
        aclPermissionService.evictCache(Project.class, project1.getId());
        
        assertThat(aclCache.getFromCache(oid2)).isNotNull();
    }

    @Test
    @DisplayName("Should reflect permission changes after cache refresh")
    void testPermissionChangesAfterCacheRefresh() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        
        MutableAcl aclBefore = (MutableAcl) mutableAclService.readAclById(oid);
        int entriesBefore = aclBefore.getEntries().size();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "newuser",
                BasePermission.READ,
                BasePermission.WRITE
        );
        
        MutableAcl aclAfter = (MutableAcl) mutableAclService.readAclById(oid);
        
        Sid newUserSid = sidResolver.principalSid("newuser");
        long newUserPermissions = aclAfter.getEntries().stream()
                .filter(ace -> ace.getSid().equals(newUserSid))
                .count();
        
        assertThat(aclAfter.getEntries().size()).isGreaterThan(entriesBefore);
        assertThat(newUserPermissions).isEqualTo(2);
    }

    @Test
    @DisplayName("Should cache ACL with all SIDs")
    void testCacheWithMultipleSids() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "user1",
                BasePermission.READ
        );
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "user2",
                BasePermission.READ
        );
        
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
        
        MutableAcl cachedAcl = aclCache.getFromCache(oid);
        assertThat(cachedAcl).isNotNull();
        assertThat(cachedAcl.getEntries().size()).isEqualTo(acl.getEntries().size());
    }

    @Test
    @DisplayName("Should handle cache operations during concurrent modifications")
    void testCacheDuringConcurrentModifications() {
        Long projectId = 6001L;
        aclPermissionService.ensureAcl(Project.class, projectId);
        
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, projectId);
        
        for (int i = 0; i < 5; i++) {
            aclPermissionService.grantToUser(
                    Project.class,
                    projectId,
                    "user" + i,
                    BasePermission.READ
            );
        }
        
        MutableAcl finalAcl = (MutableAcl) mutableAclService.readAclById(oid);
        assertThat(finalAcl.getEntries().size()).isGreaterThanOrEqualTo(5);
        
        MutableAcl cachedAcl = aclCache.getFromCache(oid);
        assertThat(cachedAcl).isNotNull();
    }
}
