package com.example.acl.service;

import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import com.example.acl.web.dto.DocumentCreateRequest;
import com.example.acl.web.dto.DocumentUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AclPermissionService aclPermissionService;

    @Transactional
    public Document createDocument(DocumentCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated to create a document");
        }

        String username = authentication.getName();
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + request.getProjectId()));

        Document document = Document.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .project(project)
                .author(author)
                .isPublic(request.isPublic())
                .build();

        document = documentRepository.save(document);
        log.info("Created document {} in project {} by user {}", document.getId(), project.getId(), username);

        aclPermissionService.applyOwnership(Document.class, document.getId(), username);
        aclPermissionService.setParent(Document.class, document.getId(), Project.class, project.getId(), true);
        log.info("Applied ACL ownership and inheritance for document {} to user {}", document.getId(), username);

        return document;
    }

    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ') or isDocumentOwner(filterObject.id) or hasProjectRole(filterObject.project.id, 'VIEWER')")
    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @PostAuthorize("hasRole('ADMIN') or hasPermission(returnObject, 'READ') or isDocumentOwner(returnObject.id) or hasProjectRole(returnObject.project.id, 'VIEWER')")
    @Transactional(readOnly = true)
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));
    }

    @PreAuthorize("hasRole('ADMIN') or isDocumentOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Document', 'WRITE')")
    @Transactional
    public Document updateDocument(Long id, DocumentUpdateRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));

        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        
        if (request.getIsPublic() != null) {
            document.setPublic(request.getIsPublic());
        }

        document = documentRepository.save(document);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        log.info("Updated document {} by user {}", id, username);

        aclPermissionService.evictCache(Document.class, id);

        return document;
    }

    @PreAuthorize("hasRole('ADMIN') or isDocumentOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Document', 'DELETE')")
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        
        documentRepository.delete(document);
        log.info("Deleted document {} by user {}", id, username);

        aclPermissionService.evictCache(Document.class, id);
    }
}
