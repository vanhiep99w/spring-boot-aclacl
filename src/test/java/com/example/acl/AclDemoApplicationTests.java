package com.example.acl;

import com.example.acl.domain.Group;
import com.example.acl.domain.Role;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AclDemoApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
        assertThat(projectRepository).isNotNull();
        assertThat(documentRepository).isNotNull();
        assertThat(commentRepository).isNotNull();
    }

    @Test
    void testDataInitialization() {
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(projectRepository.count()).isEqualTo(4);
        assertThat(documentRepository.count()).isEqualTo(5);
        assertThat(commentRepository.count()).isEqualTo(5);
    }

    @Test
    void testUserCreation() {
        var alice = userRepository.findByUsername("alice");
        assertThat(alice).isPresent();
        assertThat(alice.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(alice.get().getRole()).isEqualTo(Role.MANAGER);
        assertThat(alice.get().getGroups()).contains(Group.ENGINEERING);
        assertThat(alice.get().isEnabled()).isTrue();
    }

    @Test
    void testProjectOwnership() {
        var alice = userRepository.findByUsername("alice").orElseThrow();
        var projects = projectRepository.findByOwner(alice);
        assertThat(projects).hasSize(1);
        assertThat(projects.get(0).getName()).isEqualTo("Alice's Engineering Project");
    }

    @Test
    void testDocumentAuthorRelationship() {
        var alice = userRepository.findByUsername("alice").orElseThrow();
        var documents = documentRepository.findByAuthor(alice);
        assertThat(documents).hasSize(2);
        assertThat(documents).extracting("title")
                .containsExactlyInAnyOrder("API Design Document", "Technical Architecture Overview");
    }

    @Test
    void testCommentInheritance() {
        var documents = documentRepository.findAll();
        var apiDoc = documents.stream()
                .filter(d -> d.getTitle().equals("API Design Document"))
                .findFirst()
                .orElseThrow();

        var comments = commentRepository.findByDocument(apiDoc);
        assertThat(comments).hasSize(2);
    }

    @Test
    void testPublicProjectFlag() {
        var publicProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Public Documentation Project"))
                .findFirst()
                .orElseThrow();

        assertThat(publicProject.isPublic()).isTrue();
        assertThat(publicProject.getOwner().getUsername()).isEqualTo("admin");
    }

    @Test
    void testGroupSharing() {
        var marketingProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Marketing Campaign Project"))
                .findFirst()
                .orElseThrow();

        assertThat(marketingProject.getSharedWithGroups()).contains(Group.MARKETING);
    }

    @Test
    void testUserSharing() {
        var aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Alice's Engineering Project"))
                .findFirst()
                .orElseThrow();

        var bob = userRepository.findByUsername("bob").orElseThrow();
        assertThat(aliceProject.getSharedWith()).contains(bob);
    }
}
