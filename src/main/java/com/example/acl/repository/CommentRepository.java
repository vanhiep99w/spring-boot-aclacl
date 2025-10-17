package com.example.acl.repository;

import com.example.acl.domain.Comment;
import com.example.acl.domain.Document;
import com.example.acl.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByDocument(Document document);

    List<Comment> findByAuthor(User author);

    List<Comment> findByDocumentOrderByCreatedAtDesc(Document document);
}
