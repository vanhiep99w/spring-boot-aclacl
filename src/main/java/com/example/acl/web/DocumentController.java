package com.example.acl.web;

import com.example.acl.domain.Document;
import com.example.acl.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository documentRepository;

    /**
     * Returns only documents the current user can READ by combining ACL, ownership and ADMIN role checks.
     * PostFilter is applied element-wise on the returned collection.
     */
    @GetMapping
    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ') or isDocumentOwner(filterObject.id) or hasProjectRole(filterObject.project.id, 'VIEWER')")
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * After loading the document, enforce that the caller can READ it.
     * Combines ACL with ownership and ADMIN role as a fallback.
     */
    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or hasPermission(returnObject.body, 'READ') or isDocumentOwner(returnObject.body.id) or hasProjectRole(returnObject.body.project.id, 'VIEWER')")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Only ADMIN, the document owner, or principals with ACL WRITE on the document may update it.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isDocumentOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Document', 'WRITE')")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody Document updatedDocument) {
        return documentRepository.findById(id)
                .map(document -> {
                    document.setTitle(updatedDocument.getTitle());
                    document.setContent(updatedDocument.getContent());
                    return ResponseEntity.ok(documentRepository.save(document));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Only ADMIN, the document owner, or principals with ACL DELETE on the document may delete it.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isDocumentOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Document', 'DELETE')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(document -> {
                    documentRepository.delete(document);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
