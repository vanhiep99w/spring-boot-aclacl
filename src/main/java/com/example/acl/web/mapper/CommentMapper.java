package com.example.acl.web.mapper;

import com.example.acl.domain.Comment;
import com.example.acl.web.dto.CommentResponse;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        if (comment == null) {
            return null;
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .documentId(comment.getDocument() != null ? comment.getDocument().getId() : null)
                .documentTitle(comment.getDocument() != null ? comment.getDocument().getTitle() : null)
                .authorUsername(comment.getAuthor() != null ? comment.getAuthor().getUsername() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
