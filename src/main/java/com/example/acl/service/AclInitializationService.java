package com.example.acl.service;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.domain.Role;
import com.example.acl.security.CustomAclPermission;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AclInitializationService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final CommentRepository commentRepository;
    private final AclPermissionService aclPermissionService;

    @Bean
    @Order(2)
    CommandLineRunner initAcl() {
        return args -> {
            log.info("Initializing ACL entries...");
            bootstrapAclEntries();
            log.info("ACL initialization completed successfully!");
        };
    }

    @Transactional
    public void bootstrapAclEntries() {
        List<Project> projects = projectRepository.findAll();
        List<Document> documents = documentRepository.findAll();
        List<Comment> comments = commentRepository.findAll();

        log.debug("Creating ACL entries for {} projects, {} documents, {} comments", projects.size(), documents.size(), comments.size());

        userRepository.findByUsername("admin").orElseThrow();
        userRepository.findByUsername("alice").orElseThrow();
        userRepository.findByUsername("bob").orElseThrow();
        userRepository.findByUsername("carol").orElseThrow();
        userRepository.findByUsername("dave").orElseThrow();

        projects.forEach(this::initializeProjectAcl);
        documents.forEach(this::initializeDocumentAcl);
        comments.forEach(this::initializeCommentAcl);

        log.info("Created ACL entries for all domain objects with inheritance and group support");
    }

    private void initializeProjectAcl(Project project) {
        aclPermissionService.applyOwnership(Project.class, project.getId(), project.getOwner().getUsername());

        project.getSharedWith().forEach(user ->
                aclPermissionService.grantToUser(Project.class, project.getId(), user.getUsername(), BasePermission.READ, BasePermission.WRITE)
        );

        project.getSharedWithGroups().forEach(group ->
                aclPermissionService.grantToGroup(Project.class, project.getId(), group, BasePermission.READ, BasePermission.WRITE)
        );

        if (project.isPublic()) {
            aclPermissionService.grantToAuthority(Project.class, project.getId(), "ROLE_USER", BasePermission.READ);
        }

        aclPermissionService.grantToRole(Project.class, project.getId(), Role.MANAGER, BasePermission.READ);
    }

    private void initializeDocumentAcl(Document document) {
        aclPermissionService.applyOwnership(Document.class, document.getId(), document.getAuthor().getUsername());

        if (document.getProject() != null) {
            aclPermissionService.setParent(Document.class, document.getId(), Project.class, document.getProject().getId(), true);
        }

        document.getSharedWith().forEach(user ->
                aclPermissionService.grantToUser(Document.class, document.getId(), user.getUsername(), BasePermission.READ, BasePermission.WRITE)
        );

        document.getSharedWithGroups().forEach(group ->
                aclPermissionService.grantToGroup(Document.class, document.getId(), group, BasePermission.READ, CustomAclPermission.APPROVE)
        );

        if (document.isPublic()) {
            aclPermissionService.grantToAuthority(Document.class, document.getId(), "ROLE_USER", BasePermission.READ);
        }
    }

    private void initializeCommentAcl(Comment comment) {
        aclPermissionService.applyOwnership(Comment.class, comment.getId(), comment.getAuthor().getUsername());
        aclPermissionService.setParent(Comment.class, comment.getId(), Document.class, comment.getDocument().getId(), true);
    }
}
