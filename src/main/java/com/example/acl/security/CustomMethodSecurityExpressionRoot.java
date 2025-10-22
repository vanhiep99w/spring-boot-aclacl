package com.example.acl.security;

import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Objects;

/**
 * Custom SpEL methods for use in @PreAuthorize, @PostAuthorize and @PostFilter expressions.
 *
 * Provides convenient, domain-specific helpers that complement ACL-based checks, e.g.:
 * - isDocumentOwner(#id)
 * - isProjectOwner(#projectId)
 * - hasProjectRole(#projectId, 'VIEWER' | 'CONTRIBUTOR' | 'OWNER')
 *
 * These helpers are meant to be composed together with role checks and ACL checks, e.g.:
 * "hasRole('ADMIN') or hasPermission(#id, 'com.example.acl.domain.Document', 'WRITE') or isDocumentOwner(#id)"
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CustomMethodSecurityExpressionRoot(
            Authentication authentication,
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        super(authentication);
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public boolean isDocumentOwner(Long documentId) {
        if (documentId == null) return false;
        String username = getUsername();
        return documentRepository.findById(documentId)
                .map(Document::getAuthor)
                .map(User::getUsername)
                .filter(username::equals)
                .isPresent();
    }

    public boolean isDocumentOwner(Document document) {
        if (document == null || document.getAuthor() == null) return false;
        String username = getUsername();
        return Objects.equals(username, document.getAuthor().getUsername());
    }

    public boolean isProjectOwner(Long projectId) {
        if (projectId == null) return false;
        String username = getUsername();
        return projectRepository.findById(projectId)
                .map(Project::getOwner)
                .map(User::getUsername)
                .filter(username::equals)
                .isPresent();
    }

    public boolean hasProjectRole(Long projectId, String requiredRole) {
        if (projectId == null || requiredRole == null) return false;
        String role = requiredRole.trim().toUpperCase();
        return projectRepository.findById(projectId).map(project -> switch (role) {
            case "OWNER" -> isProjectOwner(projectId);
            case "CONTRIBUTOR" -> {
                User user = currentUser();
                boolean inUsers = project.getSharedWith().stream().anyMatch(u -> Objects.equals(u.getId(), user.getId()));
                boolean inGroups = project.getSharedWithGroups().stream().anyMatch(user.getGroups()::contains);
                yield inUsers || inGroups || isProjectOwner(projectId);
            }
            case "VIEWER" -> {
                // Viewer can be public access, contributor or owner
                boolean publicAccess = project.isPublic();
                yield publicAccess || hasProjectRole(projectId, "CONTRIBUTOR");
            }
            default -> false;
        }).orElse(false);
    }

    private User currentUser() {
        String username = getUsername();
        return userRepository.findByUsername(username).orElse(null);
    }

    private String getUsername() {
        return this.getAuthentication() != null ? this.getAuthentication().getName() : null;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return this.filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return this.returnObject;
    }

    @Override
    public Object getThis() {
        return this.target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}
