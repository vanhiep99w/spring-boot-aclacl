package com.example.acl;

import com.example.acl.domain.Project;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.service.AclPermissionService;
import com.example.acl.web.dto.AccessibleResourcesResponse;
import com.example.acl.web.dto.BulkPermissionUpdateRequest;
import com.example.acl.web.dto.EffectivePermissionsResponse;
import com.example.acl.web.dto.PermissionGrantRequest;
import com.example.acl.web.dto.PermissionInheritanceResponse;
import com.example.acl.web.dto.PermissionResponse;
import com.example.acl.web.dto.PermissionRevokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PermissionManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AclPermissionService aclPermissionService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGrantPermission() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        PermissionGrantRequest request = PermissionGrantRequest.builder()
                .resourceType("PROJECT")
                .resourceId(project.getId())
                .subjectType("USER")
                .subjectIdentifier("bob")
                .permissions(List.of("READ", "WRITE"))
                .build();

        mockMvc.perform(post("/api/permissions/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resourceType").value("PROJECT"))
                .andExpect(jsonPath("$.resourceId").value(project.getId()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRevokePermission() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        aclPermissionService.grantToUser(Project.class, project.getId(), "carol", BasePermission.READ);

        PermissionRevokeRequest request = PermissionRevokeRequest.builder()
                .resourceType("PROJECT")
                .resourceId(project.getId())
                .subjectType("USER")
                .subjectIdentifier("carol")
                .permissions(List.of("READ"))
                .build();

        mockMvc.perform(post("/api/permissions/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBulkUpdateGrant() throws Exception {
        List<Project> projects = projectRepository.findAll();
        List<Long> projectIds = projects.stream().limit(2).map(Project::getId).toList();

        BulkPermissionUpdateRequest request = BulkPermissionUpdateRequest.builder()
                .resourceType("PROJECT")
                .resourceIds(projectIds)
                .subjectType("GROUP")
                .subjectIdentifier("ENGINEERING")
                .operation("GRANT")
                .permissions(List.of("READ"))
                .build();

        mockMvc.perform(post("/api/permissions/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resourcesAffected").value(projectIds.size()));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    void testCheckEffectivePermissions() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        MvcResult result = mockMvc.perform(get("/api/permissions/check")
                        .param("resourceType", "PROJECT")
                        .param("resourceId", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("PROJECT"))
                .andExpect(jsonPath("$.resourceId").value(project.getId()))
                .andExpect(jsonPath("$.subject").value("alice"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        EffectivePermissionsResponse response = objectMapper.readValue(content, EffectivePermissionsResponse.class);
        assertThat(response.getGrantedPermissions()).isNotNull();
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    void testListAccessibleResources() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/permissions/accessible")
                        .param("resourceType", "PROJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"))
                .andExpect(jsonPath("$.resourceType").value("PROJECT"))
                .andExpect(jsonPath("$.totalCount").exists())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        AccessibleResourcesResponse response = objectMapper.readValue(content, AccessibleResourcesResponse.class);
        assertThat(response.getResources()).isNotNull();
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    void testCheckInheritance() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        MvcResult result = mockMvc.perform(get("/api/permissions/inheritance")
                        .param("resourceType", "PROJECT")
                        .param("resourceId", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("PROJECT"))
                .andExpect(jsonPath("$.resourceId").value(project.getId()))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        PermissionInheritanceResponse response = objectMapper.readValue(content, PermissionInheritanceResponse.class);
        assertThat(response.getDirectPermissions()).isNotNull();
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    void testListAvailablePermissions() throws Exception {
        mockMvc.perform(get("/api/permissions/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCustomPermissionDemo() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/permissions/custom-demo")
                        .param("resourceType", "PROJECT")
                        .param("resourceId", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customPermissions").isArray())
                .andExpect(jsonPath("$.customPermissions[0]").value("SHARE"));
    }

    @Test
    @WithMockUser(username = "dave", roles = {"VIEWER"})
    void testGrantPermissionForbiddenForViewer() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        PermissionGrantRequest request = PermissionGrantRequest.builder()
                .resourceType("PROJECT")
                .resourceId(project.getId())
                .subjectType("USER")
                .subjectIdentifier("bob")
                .permissions(List.of("READ"))
                .build();

        mockMvc.perform(post("/api/permissions/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGrantInvalidResourceType() throws Exception {
        PermissionGrantRequest request = PermissionGrantRequest.builder()
                .resourceType("INVALID")
                .resourceId(1L)
                .subjectType("USER")
                .subjectIdentifier("bob")
                .permissions(List.of("READ"))
                .build();

        mockMvc.perform(post("/api/permissions/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGrantCustomPermission() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        PermissionGrantRequest request = PermissionGrantRequest.builder()
                .resourceType("PROJECT")
                .resourceId(project.getId())
                .subjectType("USER")
                .subjectIdentifier("bob")
                .permissions(List.of("SHARE", "APPROVE"))
                .build();

        mockMvc.perform(post("/api/permissions/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
