package com.example.acl.service;

import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import com.example.acl.web.dto.ProjectCreateRequest;
import com.example.acl.web.dto.ProjectUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AclPermissionService aclPermissionService;

    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated to create a project");
        }

        String username = authentication.getName();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .isPublic(request.isPublic())
                .build();

        project = projectRepository.save(project);
        log.info("Created project {} by user {}", project.getId(), username);

        aclPermissionService.applyOwnership(Project.class, project.getId(), username);
        log.info("Applied ACL ownership for project {} to user {}", project.getId(), username);

        return project;
    }

    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ') or isProjectOwner(filterObject.id) or hasProjectRole(filterObject.id, 'VIEWER')")
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @PostAuthorize("hasRole('ADMIN') or hasPermission(returnObject, 'READ') or isProjectOwner(returnObject.id) or hasProjectRole(returnObject.id, 'VIEWER')")
    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));
    }

    @PreAuthorize("hasRole('ADMIN') or isProjectOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Project', 'WRITE')")
    @Transactional
    public Project updateProject(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        
        if (request.getIsPublic() != null) {
            project.setPublic(request.getIsPublic());
        }

        project = projectRepository.save(project);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        log.info("Updated project {} by user {}", id, username);

        aclPermissionService.evictCache(Project.class, id);

        return project;
    }

    @PreAuthorize("hasRole('ADMIN') or isProjectOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Project', 'DELETE')")
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        
        projectRepository.delete(project);
        log.info("Deleted project {} by user {}", id, username);

        aclPermissionService.evictCache(Project.class, id);
    }
}
