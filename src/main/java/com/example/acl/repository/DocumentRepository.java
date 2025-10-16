package com.example.acl.repository;

import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByProject(Project project);

    List<Document> findByAuthor(User author);

    @Query("SELECT d FROM Document d WHERE d.author = :user OR :user MEMBER OF d.sharedWith")
    List<Document> findAccessibleByUser(@Param("user") User user);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.comments WHERE d.id = :id")
    Optional<Document> findByIdWithComments(@Param("id") Long id);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.sharedWith WHERE d.id = :id")
    Optional<Document> findByIdWithSharedUsers(@Param("id") Long id);
}
