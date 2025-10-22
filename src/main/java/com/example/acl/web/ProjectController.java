package com.example.acl.web;

import com.example.acl.domain.Project;
import com.example.acl.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;

    /**
     * Return only projects the caller can READ. Combines ACL, project roles and ADMIN role.
     */
    @GetMapping
    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ') or isProjectOwner(filterObject.id) or hasProjectRole(filterObject.id, 'VIEWER')")
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Verify access after loading project.
     */
    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or hasPermission(returnObject.body, 'READ') or isProjectOwner(returnObject.body.id) or hasProjectRole(returnObject.body.id, 'VIEWER')")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Only ADMIN, project owner, or principals with ACL WRITE may update.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isProjectOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Project', 'WRITE')")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project updatedProject) {
        return projectRepository.findById(id)
                .map(project -> {
                    project.setName(updatedProject.getName());
                    project.setDescription(updatedProject.getDescription());
                    return ResponseEntity.ok(projectRepository.save(project));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Only ADMIN, project owner, or principals with ACL DELETE may delete.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isProjectOwner(#id) or hasPermission(#id, 'com.example.acl.domain.Project', 'DELETE')")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(project -> {
                    projectRepository.delete(project);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
