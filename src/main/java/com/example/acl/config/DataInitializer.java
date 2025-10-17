package com.example.acl.config;

import com.example.acl.domain.*;
import com.example.acl.repository.CommentRepository;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final CommentRepository commentRepository;

    @Bean
    @Order(1)
    CommandLineRunner initDatabase(PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Initializing database with sample data...");

            User adminUser = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .groups(Set.of(Group.EXECUTIVE, Group.ENGINEERING))
                    .enabled(true)
                    .build();
            adminUser = userRepository.save(adminUser);
            log.debug("Created admin user: {}", adminUser.getUsername());

            User aliceUser = User.builder()
                    .username("alice")
                    .email("alice@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.MANAGER)
                    .groups(Set.of(Group.ENGINEERING))
                    .enabled(true)
                    .build();
            aliceUser = userRepository.save(aliceUser);
            log.debug("Created user: {}", aliceUser.getUsername());

            User bobUser = User.builder()
                    .username("bob")
                    .email("bob@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.MEMBER)
                    .groups(Set.of(Group.ENGINEERING))
                    .enabled(true)
                    .build();
            bobUser = userRepository.save(bobUser);
            log.debug("Created user: {}", bobUser.getUsername());

            User carolUser = User.builder()
                    .username("carol")
                    .email("carol@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.MEMBER)
                    .groups(Set.of(Group.MARKETING))
                    .enabled(true)
                    .build();
            carolUser = userRepository.save(carolUser);
            log.debug("Created user: {}", carolUser.getUsername());

            User daveUser = User.builder()
                    .username("dave")
                    .email("dave@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.VIEWER)
                    .groups(Set.of(Group.SALES))
                    .enabled(true)
                    .build();
            daveUser = userRepository.save(daveUser);
            log.debug("Created user: {}", daveUser.getUsername());

            Project aliceProject = Project.builder()
                    .name("Alice's Engineering Project")
                    .description("A project owned by Alice, shared with the Engineering group")
                    .owner(aliceUser)
                    .sharedWith(Set.of(bobUser))
                    .sharedWithGroups(Set.of(Group.ENGINEERING))
                    .isPublic(false)
                    .build();
            aliceProject = projectRepository.save(aliceProject);
            log.debug("Created project: {} (owner: {})", aliceProject.getName(), aliceProject.getOwner().getUsername());

            Project bobProject = Project.builder()
                    .name("Bob's Internal Project")
                    .description("A private project owned by Bob")
                    .owner(bobUser)
                    .isPublic(false)
                    .build();
            bobProject = projectRepository.save(bobProject);
            log.debug("Created project: {} (owner: {})", bobProject.getName(), bobProject.getOwner().getUsername());

            Project publicProject = Project.builder()
                    .name("Public Documentation Project")
                    .description("A public project accessible to everyone")
                    .owner(adminUser)
                    .isPublic(true)
                    .build();
            publicProject = projectRepository.save(publicProject);
            log.debug("Created project: {} (owner: {}, public: {})", publicProject.getName(), publicProject.getOwner().getUsername(), publicProject.isPublic());

            Project marketingProject = Project.builder()
                    .name("Marketing Campaign Project")
                    .description("A project shared with Marketing group")
                    .owner(carolUser)
                    .sharedWithGroups(Set.of(Group.MARKETING))
                    .isPublic(false)
                    .build();
            marketingProject = projectRepository.save(marketingProject);
            log.debug("Created project: {} (owner: {})", marketingProject.getName(), marketingProject.getOwner().getUsername());

            Document aliceDoc1 = Document.builder()
                    .title("API Design Document")
                    .content("This document describes the API design for the new microservice architecture.")
                    .project(aliceProject)
                    .author(aliceUser)
                    .sharedWith(Set.of(bobUser))
                    .isPublic(false)
                    .build();
            aliceDoc1 = documentRepository.save(aliceDoc1);
            log.debug("Created document: {} (author: {}, project: {})", aliceDoc1.getTitle(), aliceDoc1.getAuthor().getUsername(), aliceDoc1.getProject().getName());

            Document aliceDoc2 = Document.builder()
                    .title("Technical Architecture Overview")
                    .content("Overview of the system architecture with diagrams and component descriptions.")
                    .project(aliceProject)
                    .author(aliceUser)
                    .sharedWithGroups(Set.of(Group.ENGINEERING))
                    .isPublic(false)
                    .build();
            aliceDoc2 = documentRepository.save(aliceDoc2);
            log.debug("Created document: {} (author: {}, project: {})", aliceDoc2.getTitle(), aliceDoc2.getAuthor().getUsername(), aliceDoc2.getProject().getName());

            Document bobDoc1 = Document.builder()
                    .title("Bob's Private Notes")
                    .content("Personal notes and ideas for future development.")
                    .project(bobProject)
                    .author(bobUser)
                    .isPublic(false)
                    .build();
            bobDoc1 = documentRepository.save(bobDoc1);
            log.debug("Created document: {} (author: {}, project: {})", bobDoc1.getTitle(), bobDoc1.getAuthor().getUsername(), bobDoc1.getProject().getName());

            Document publicDoc1 = Document.builder()
                    .title("Getting Started Guide")
                    .content("Welcome to our platform! This guide will help you get started quickly.")
                    .project(publicProject)
                    .author(adminUser)
                    .isPublic(true)
                    .build();
            publicDoc1 = documentRepository.save(publicDoc1);
            log.debug("Created document: {} (author: {}, project: {}, public: {})", publicDoc1.getTitle(), publicDoc1.getAuthor().getUsername(), publicDoc1.getProject().getName(), publicDoc1.isPublic());

            Document marketingDoc1 = Document.builder()
                    .title("Q4 Marketing Strategy")
                    .content("Strategic plan for Q4 marketing campaigns and initiatives.")
                    .project(marketingProject)
                    .author(carolUser)
                    .sharedWithGroups(Set.of(Group.MARKETING))
                    .isPublic(false)
                    .build();
            marketingDoc1 = documentRepository.save(marketingDoc1);
            log.debug("Created document: {} (author: {}, project: {})", marketingDoc1.getTitle(), marketingDoc1.getAuthor().getUsername(), marketingDoc1.getProject().getName());

            Comment comment1 = Comment.builder()
                    .content("Great document! I have a few suggestions on the authentication flow.")
                    .document(aliceDoc1)
                    .author(bobUser)
                    .build();
            comment1 = commentRepository.save(comment1);
            log.debug("Created comment on '{}' by {}", comment1.getDocument().getTitle(), comment1.getAuthor().getUsername());

            Comment comment2 = Comment.builder()
                    .content("Thanks Bob! Let's discuss this in tomorrow's standup.")
                    .document(aliceDoc1)
                    .author(aliceUser)
                    .build();
            comment2 = commentRepository.save(comment2);
            log.debug("Created comment on '{}' by {}", comment2.getDocument().getTitle(), comment2.getAuthor().getUsername());

            Comment comment3 = Comment.builder()
                    .content("The architecture looks solid. Should we consider adding a caching layer?")
                    .document(aliceDoc2)
                    .author(bobUser)
                    .build();
            comment3 = commentRepository.save(comment3);
            log.debug("Created comment on '{}' by {}", comment3.getDocument().getTitle(), comment3.getAuthor().getUsername());

            Comment comment4 = Comment.builder()
                    .content("This guide is very helpful for new users!")
                    .document(publicDoc1)
                    .author(daveUser)
                    .build();
            comment4 = commentRepository.save(comment4);
            log.debug("Created comment on '{}' by {}", comment4.getDocument().getTitle(), comment4.getAuthor().getUsername());

            Comment comment5 = Comment.builder()
                    .content("We should focus on social media presence in Q4.")
                    .document(marketingDoc1)
                    .author(carolUser)
                    .build();
            comment5 = commentRepository.save(comment5);
            log.debug("Created comment on '{}' by {}", comment5.getDocument().getTitle(), comment5.getAuthor().getUsername());

            log.info("Database initialization completed successfully!");
            log.info("Created {} users, {} projects, {} documents, {} comments",
                    userRepository.count(),
                    projectRepository.count(),
                    documentRepository.count(),
                    commentRepository.count());
        };
    }
}
