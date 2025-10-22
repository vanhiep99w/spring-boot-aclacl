package com.example.acl.web;

import com.example.acl.domain.Comment;
import com.example.acl.service.CommentService;
import com.example.acl.web.dto.CommentCreateRequest;
import com.example.acl.web.dto.CommentResponse;
import com.example.acl.web.dto.CommentUpdateRequest;
import com.example.acl.web.mapper.CommentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentCreateRequest request) {
        Comment comment = commentService.createComment(request);
        CommentResponse response = commentMapper.toResponse(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAllComments() {
        List<Comment> comments = commentService.getAllComments();
        List<CommentResponse> responses = comments.stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long id) {
        Comment comment = commentService.getCommentById(id);
        CommentResponse response = commentMapper.toResponse(comment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByDocument(@PathVariable Long documentId) {
        List<Comment> comments = commentService.getCommentsByDocumentId(documentId);
        List<CommentResponse> responses = comments.stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequest request) {
        Comment comment = commentService.updateComment(id, request);
        CommentResponse response = commentMapper.toResponse(comment);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
