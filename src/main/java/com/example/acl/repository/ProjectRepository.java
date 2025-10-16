package com.example.acl.repository;

import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwner(User owner);

    @Query("SELECT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.sharedWith")
    List<Project> findAccessibleByUser(@Param("user") User user);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.sharedWith WHERE p.id = :id")
    Optional<Project> findByIdWithSharedUsers(@Param("id") Long id);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.documents WHERE p.id = :id")
    Optional<Project> findByIdWithDocuments(@Param("id") Long id);
}
