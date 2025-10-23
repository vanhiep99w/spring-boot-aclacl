package com.example.acl.service;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.web.dto.AccessibleResourcesResponse;
import com.example.acl.web.dto.EffectivePermissionsResponse;
import com.example.acl.web.dto.PermissionInheritanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionDiscoveryService {

    private final MutableAclService aclService;
    private final AclPermissionRegistry permissionRegistry;
    private final AclSidResolver sidResolver;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final CommentRepository commentRepository;
    private final SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    @Transactional(readOnly = true)
    public EffectivePermissionsResponse getEffectivePermissions(Class<?> domainClass, Serializable identifier, Authentication authentication) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, identifier);
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);

        EffectivePermissionsResponse.EffectivePermissionsResponseBuilder builder = EffectivePermissionsResponse.builder()
                .resourceType(domainClass.getSimpleName())
                .resourceId((Long) identifier)
                .subject(authentication.getName())
                .hasAccess(false);

        try {
            Acl acl = aclService.readAclById(oid, sids);
            
            List<String> directPermissions = new ArrayList<>();
            List<String> inheritedPermissions = new ArrayList<>();
            
            for (AccessControlEntry ace : acl.getEntries()) {
                if (sids.contains(ace.getSid()) && ace.isGranting()) {
                    String permName = permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN");
                    directPermissions.add(permName);
                }
            }
            
            if (acl.isEntriesInheriting() && acl.getParentAcl() != null) {
                Acl parentAcl = acl.getParentAcl();
                for (AccessControlEntry ace : parentAcl.getEntries()) {
                    if (sids.contains(ace.getSid()) && ace.isGranting()) {
                        String permName = permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN");
                        inheritedPermissions.add(permName);
                    }
                }
                builder.parentResource(parentAcl.getObjectIdentity().getType() + ":" + parentAcl.getObjectIdentity().getIdentifier());
            }
            
            builder.grantedPermissions(directPermissions)
                   .inheritedPermissions(inheritedPermissions)
                   .hasAccess(!directPermissions.isEmpty() || !inheritedPermissions.isEmpty());
            
        } catch (NotFoundException ex) {
            log.debug("No ACL found for {} with id {}", domainClass.getSimpleName(), identifier);
            builder.grantedPermissions(List.of())
                   .inheritedPermissions(List.of());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public AccessibleResourcesResponse findAccessibleResources(String resourceType, Authentication authentication) {
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
        
        List<AccessibleResourcesResponse.ResourcePermissionInfo> accessibleResources = new ArrayList<>();
        
        switch (resourceType.toUpperCase()) {
            case "PROJECT" -> accessibleResources = findAccessibleProjects(sids);
            case "DOCUMENT" -> accessibleResources = findAccessibleDocuments(sids);
            case "COMMENT" -> accessibleResources = findAccessibleComments(sids);
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        }
        
        return AccessibleResourcesResponse.builder()
                .subject(authentication.getName())
                .resourceType(resourceType)
                .resources(accessibleResources)
                .totalCount(accessibleResources.size())
                .build();
    }

    @Transactional(readOnly = true)
    public PermissionInheritanceResponse getPermissionInheritance(Class<?> domainClass, Serializable identifier) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, identifier);
        List<Sid> sids = sidResolver.currentAuthenticationSids();

        try {
            Acl acl = aclService.readAclById(oid, sids);
            
            List<String> directPermissions = acl.getEntries().stream()
                    .filter(AccessControlEntry::isGranting)
                    .map(ace -> permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN"))
                    .distinct()
                    .collect(Collectors.toList());
            
            String resourceName = getResourceName(domainClass, identifier);
            
            PermissionInheritanceResponse.PermissionInheritanceResponseBuilder builder = PermissionInheritanceResponse.builder()
                    .resourceType(domainClass.getSimpleName())
                    .resourceId((Long) identifier)
                    .resourceName(resourceName)
                    .directPermissions(directPermissions)
                    .entriesInheriting(acl.isEntriesInheriting())
                    .hasParent(acl.getParentAcl() != null);
            
            if (acl.getParentAcl() != null) {
                Acl parentAcl = acl.getParentAcl();
                List<String> inheritedPermissions = parentAcl.getEntries().stream()
                        .filter(AccessControlEntry::isGranting)
                        .map(ace -> permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN"))
                        .distinct()
                        .collect(Collectors.toList());
                
                String parentName = getResourceName(
                        getClassForType(parentAcl.getObjectIdentity().getType()),
                        (Serializable) parentAcl.getObjectIdentity().getIdentifier()
                );
                
                builder.parent(PermissionInheritanceResponse.ParentResourceInfo.builder()
                        .resourceType(parentAcl.getObjectIdentity().getType())
                        .resourceId((Long) parentAcl.getObjectIdentity().getIdentifier())
                        .resourceName(parentName)
                        .permissions(inheritedPermissions)
                        .build())
                       .inheritedPermissions(inheritedPermissions);
            } else {
                builder.inheritedPermissions(List.of());
            }
            
            return builder.build();
            
        } catch (NotFoundException ex) {
            log.debug("No ACL found for {} with id {}", domainClass.getSimpleName(), identifier);
            return PermissionInheritanceResponse.builder()
                    .resourceType(domainClass.getSimpleName())
                    .resourceId((Long) identifier)
                    .resourceName(getResourceName(domainClass, identifier))
                    .directPermissions(List.of())
                    .inheritedPermissions(List.of())
                    .hasParent(false)
                    .entriesInheriting(false)
                    .build();
        }
    }

    private List<AccessibleResourcesResponse.ResourcePermissionInfo> findAccessibleProjects(List<Sid> sids) {
        List<Project> allProjects = projectRepository.findAll();
        List<AccessibleResourcesResponse.ResourcePermissionInfo> accessible = new ArrayList<>();
        
        for (Project project : allProjects) {
            ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
            try {
                Acl acl = aclService.readAclById(oid, sids);
                Set<String> permissions = getPermissionsForSids(acl, sids);
                if (!permissions.isEmpty()) {
                    accessible.add(AccessibleResourcesResponse.ResourcePermissionInfo.builder()
                            .resourceId(project.getId())
                            .resourceName(project.getName())
                            .permissions(new ArrayList<>(permissions))
                            .accessSource("ACL")
                            .build());
                }
            } catch (NotFoundException ex) {
                log.trace("No ACL for project {}", project.getId());
            }
        }
        return accessible;
    }

    private List<AccessibleResourcesResponse.ResourcePermissionInfo> findAccessibleDocuments(List<Sid> sids) {
        List<Document> allDocuments = documentRepository.findAll();
        List<AccessibleResourcesResponse.ResourcePermissionInfo> accessible = new ArrayList<>();
        
        for (Document document : allDocuments) {
            ObjectIdentity oid = new ObjectIdentityImpl(Document.class, document.getId());
            try {
                Acl acl = aclService.readAclById(oid, sids);
                Set<String> permissions = getPermissionsForSids(acl, sids);
                if (!permissions.isEmpty()) {
                    accessible.add(AccessibleResourcesResponse.ResourcePermissionInfo.builder()
                            .resourceId(document.getId())
                            .resourceName(document.getTitle())
                            .permissions(new ArrayList<>(permissions))
                            .accessSource(acl.isEntriesInheriting() && acl.getParentAcl() != null ? "Inherited" : "Direct")
                            .build());
                }
            } catch (NotFoundException ex) {
                log.trace("No ACL for document {}", document.getId());
            }
        }
        return accessible;
    }

    private List<AccessibleResourcesResponse.ResourcePermissionInfo> findAccessibleComments(List<Sid> sids) {
        List<Comment> allComments = commentRepository.findAll();
        List<AccessibleResourcesResponse.ResourcePermissionInfo> accessible = new ArrayList<>();
        
        for (Comment comment : allComments) {
            ObjectIdentity oid = new ObjectIdentityImpl(Comment.class, comment.getId());
            try {
                Acl acl = aclService.readAclById(oid, sids);
                Set<String> permissions = getPermissionsForSids(acl, sids);
                if (!permissions.isEmpty()) {
                    accessible.add(AccessibleResourcesResponse.ResourcePermissionInfo.builder()
                            .resourceId(comment.getId())
                            .resourceName("Comment on: " + comment.getDocument().getTitle())
                            .permissions(new ArrayList<>(permissions))
                            .accessSource(acl.isEntriesInheriting() && acl.getParentAcl() != null ? "Inherited" : "Direct")
                            .build());
                }
            } catch (NotFoundException ex) {
                log.trace("No ACL for comment {}", comment.getId());
            }
        }
        return accessible;
    }

    private Set<String> getPermissionsForSids(Acl acl, List<Sid> sids) {
        Set<String> permissions = new HashSet<>();
        for (AccessControlEntry ace : acl.getEntries()) {
            if (sids.contains(ace.getSid()) && ace.isGranting()) {
                String permName = permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN");
                permissions.add(permName);
            }
        }
        
        if (acl.isEntriesInheriting() && acl.getParentAcl() != null) {
            for (AccessControlEntry ace : acl.getParentAcl().getEntries()) {
                if (sids.contains(ace.getSid()) && ace.isGranting()) {
                    String permName = permissionRegistry.resolveName(ace.getPermission()).orElse("UNKNOWN");
                    permissions.add(permName);
                }
            }
        }
        return permissions;
    }

    private String getResourceName(Class<?> domainClass, Serializable identifier) {
        if (domainClass.equals(Project.class)) {
            return projectRepository.findById((Long) identifier)
                    .map(Project::getName)
                    .orElse("Unknown");
        } else if (domainClass.equals(Document.class)) {
            return documentRepository.findById((Long) identifier)
                    .map(Document::getTitle)
                    .orElse("Unknown");
        } else if (domainClass.equals(Comment.class)) {
            return commentRepository.findById((Long) identifier)
                    .map(c -> "Comment #" + c.getId())
                    .orElse("Unknown");
        }
        return "Unknown";
    }

    private Class<?> getClassForType(String type) {
        return switch (type) {
            case "com.example.acl.domain.Project" -> Project.class;
            case "com.example.acl.domain.Document" -> Document.class;
            case "com.example.acl.domain.Comment" -> Comment.class;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
