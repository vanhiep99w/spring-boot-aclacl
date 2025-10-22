package com.example.acl.service;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.User;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.UserRepository;
import com.example.acl.security.CustomAclPermission;
import com.example.acl.web.dto.CommentCreateRequest;
import com.example.acl.web.dto.CommentUpdateRequest;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AclPermissionService aclPermissionService;

    @Transactional
    public Comment createComment(CommentCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated to create a comment");
        }

        String username = authentication.getName();
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + request.getDocumentId()));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .document(document)
                .author(author)
                .build();

        comment = commentRepository.save(comment);
        log.info("Created comment {} on document {} by user {}", comment.getId(), document.getId(), username);

        aclPermissionService.applyOwnership(Comment.class, comment.getId(), username);
        aclPermissionService.setParent(Comment.class, comment.getId(), Document.class, document.getId(), true);
        log.info("Applied ACL ownership and inheritance for comment {} to user {}", comment.getId(), username);

        return comment;
    }

    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ') or filterObject.author.username == authentication.name")
    @Transactional(readOnly = true)
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    @PostAuthorize("hasRole('ADMIN') or hasPermission(returnObject, 'READ') or returnObject.author.username == authentication.name")
    @Transactional(readOnly = true)
    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByDocumentId(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + documentId));
        return commentRepository.findByDocumentOrderByCreatedAtDesc(document);
    }

    @Transactional
    public Comment updateComment(Long id, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().getUsername().equals(username);
        boolean hasWritePermission = aclPermissionService.hasPermission(
                authentication, Comment.class, comment.getId(), CustomAclPermission.WRITE);

        if (!isAdmin && !isAuthor && !hasWritePermission) {
            throw new AccessDeniedException("You do not have permission to update this comment");
        }

        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);
        log.info("Updated comment {} by user {}", id, username);

        aclPermissionService.evictCache(Comment.class, id);

        return comment;
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().getUsername().equals(username);
        boolean hasDeletePermission = aclPermissionService.hasPermission(
                authentication, Comment.class, comment.getId(), CustomAclPermission.DELETE);

        if (!isAdmin && !isAuthor && !hasDeletePermission) {
            throw new AccessDeniedException("You do not have permission to delete this comment");
        }
        
        commentRepository.delete(comment);
        log.info("Deleted comment {} by user {}", id, username);

        aclPermissionService.evictCache(Comment.class, id);
    }
}
