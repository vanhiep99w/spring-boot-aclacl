package com.example.acl.web.mapper;

import com.example.acl.domain.Document;
import com.example.acl.web.dto.DocumentResponse;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .projectId(document.getProject() != null ? document.getProject().getId() : null)
                .projectName(document.getProject() != null ? document.getProject().getName() : null)
                .authorUsername(document.getAuthor() != null ? document.getAuthor().getUsername() : null)
                .isPublic(document.isPublic())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
