package com.example.acl.service;

import com.example.acl.domain.Document;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AclInitializationService {

    private final MutableAclService aclService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;

    @Bean
    @Order(2)
    CommandLineRunner initAcl() {
        return args -> {
            log.info("Initializing ACL entries...");
            bootstrapAclEntries();
            log.info("ACL initialization completed successfully!");
        };
    }

    @Transactional
    public void bootstrapAclEntries() {
        List<User> users = userRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        List<Document> documents = documentRepository.findAll();

        log.debug("Creating ACL entries for {} users, {} projects, {} documents", 
                users.size(), projects.size(), documents.size());

        User admin = userRepository.findByUsername("admin").orElseThrow();
        User alice = userRepository.findByUsername("alice").orElseThrow();
        User bob = userRepository.findByUsername("bob").orElseThrow();
        User carol = userRepository.findByUsername("carol").orElseThrow();
        User dave = userRepository.findByUsername("dave").orElseThrow();

        for (Project project : projects) {
            createAclForProject(project);
        }

        for (Document document : documents) {
            createAclForDocument(document);
        }

        log.info("Created ACL entries for all domain objects");
    }

    private void createAclForProject(Project project) {
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, project.getId());
        
        try {
            MutableAcl acl = aclService.readAclById(oid);
            log.debug("ACL already exists for project: {}", project.getName());
            return;
        } catch (NotFoundException e) {
        }

        MutableAcl acl = aclService.createAcl(oid);
        
        PrincipalSid ownerSid = new PrincipalSid(project.getOwner().getUsername());
        acl.setOwner(ownerSid);
        
        acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.DELETE, ownerSid, true);
        
        if (project.isPublic()) {
            PrincipalSid everyoneSid = new PrincipalSid("ROLE_USER");
            acl.insertAce(acl.getEntries().size(), BasePermission.READ, everyoneSid, true);
        }
        
        project.getSharedWith().forEach(user -> {
            PrincipalSid userSid = new PrincipalSid(user.getUsername());
            acl.insertAce(acl.getEntries().size(), BasePermission.READ, userSid, true);
            acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, userSid, true);
        });
        
        aclService.updateAcl(acl);
        log.debug("Created ACL for project: {} (owner: {})", project.getName(), project.getOwner().getUsername());
    }

    private void createAclForDocument(Document document) {
        ObjectIdentity oid = new ObjectIdentityImpl(Document.class, document.getId());
        
        try {
            MutableAcl acl = aclService.readAclById(oid);
            log.debug("ACL already exists for document: {}", document.getTitle());
            return;
        } catch (NotFoundException e) {
        }

        MutableAcl acl = aclService.createAcl(oid);
        
        PrincipalSid ownerSid = new PrincipalSid(document.getAuthor().getUsername());
        acl.setOwner(ownerSid);
        
        acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, ownerSid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.DELETE, ownerSid, true);
        
        if (document.isPublic()) {
            PrincipalSid everyoneSid = new PrincipalSid("ROLE_USER");
            acl.insertAce(acl.getEntries().size(), BasePermission.READ, everyoneSid, true);
        }
        
        document.getSharedWith().forEach(user -> {
            PrincipalSid userSid = new PrincipalSid(user.getUsername());
            acl.insertAce(acl.getEntries().size(), BasePermission.READ, userSid, true);
            acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, userSid, true);
        });
        
        aclService.updateAcl(acl);
        log.debug("Created ACL for document: {} (author: {})", document.getTitle(), document.getAuthor().getUsername());
    }

    public void grantPermission(Class<?> domainClass, Long objectId, String username, Permission permission) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, objectId);
        
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oid);
        } catch (NotFoundException e) {
            acl = aclService.createAcl(oid);
        }
        
        PrincipalSid sid = new PrincipalSid(username);
        acl.insertAce(acl.getEntries().size(), permission, sid, true);
        aclService.updateAcl(acl);
    }

    public void revokePermission(Class<?> domainClass, Long objectId, String username, Permission permission) {
        ObjectIdentity oid = new ObjectIdentityImpl(domainClass, objectId);
        
        MutableAcl acl = (MutableAcl) aclService.readAclById(oid);
        PrincipalSid sid = new PrincipalSid(username);
        
        List<AccessControlEntry> entries = acl.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            AccessControlEntry entry = entries.get(i);
            if (entry.getSid().equals(sid) && entry.getPermission().equals(permission)) {
                acl.deleteAce(i);
                break;
            }
        }
        
        aclService.updateAcl(acl);
    }
}
