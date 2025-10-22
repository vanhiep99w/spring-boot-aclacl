package com.example.acl.web.mapper;

import com.example.acl.domain.Project;
import com.example.acl.web.dto.ProjectResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        if (project == null) {
            return null;
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerUsername(project.getOwner() != null ? project.getOwner().getUsername() : null)
                .isPublic(project.isPublic())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
