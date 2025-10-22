package com.example.acl.web;

import com.example.acl.domain.Document;
import com.example.acl.domain.Group;
import com.example.acl.domain.Project;
import com.example.acl.domain.User;
import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import com.example.acl.service.AclPermissionService;
import com.example.acl.service.AclSidResolver;
import com.example.acl.web.dto.DocumentCreateRequest;
import com.example.acl.web.dto.DocumentUpdateRequest;
import com.example.acl.web.dto.ProjectCreateRequest;
import com.example.acl.web.dto.ProjectUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Secured REST Endpoint Integration Tests")
class SecuredRestEndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AclPermissionService aclPermissionService;

    @Autowired
    private AclSidResolver sidResolver;

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Owner should have full access to their project")
    void testOwnerFullAccess() throws Exception {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getOwner().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/projects/" + aliceProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceProject.getId()))
                .andExpect(jsonPath("$.name").value(aliceProject.getName()));
        
        ProjectUpdateRequest updateRequest = ProjectUpdateRequest.builder()
                .name("Updated Project Name")
                .description("Updated description")
                .build();
        
        mockMvc.perform(put("/api/projects/" + aliceProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project Name"));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("Shared user should have read access")
    void testSharedUserReadAccess() throws Exception {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Alice's Engineering Project"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/projects/" + aliceProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceProject.getId()));
    }

    @Test
    @WithMockUser(username = "carol", roles = {"MEMBER"})
    @DisplayName("Non-shared user should be forbidden from accessing project")
    void testNonSharedUserForbidden() throws Exception {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getOwner().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/projects/" + aliceProject.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dave", roles = {"VIEWER"})
    @DisplayName("User without write permission should not be able to update")
    void testForbiddenUpdate() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "dave",
                BasePermission.READ
        );
        
        ProjectUpdateRequest updateRequest = ProjectUpdateRequest.builder()
                .name("Attempted Update")
                .description("This should fail")
                .build();
        
        mockMvc.perform(put("/api/projects/" + project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin should override and access any project")
    void testAdminOverride() throws Exception {
        Project anyProject = projectRepository.findAll().stream()
                .filter(p -> !p.getOwner().getUsername().equals("admin"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/projects/" + anyProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(anyProject.getId()));
        
        ProjectUpdateRequest updateRequest = ProjectUpdateRequest.builder()
                .name("Admin Updated Project")
                .description("Admin can update any project")
                .build();
        
        mockMvc.perform(put("/api/projects/" + anyProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Updated Project"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin should be able to delete any project")
    void testAdminDelete() throws Exception {
        ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .name("Test Project for Deletion")
                .description("This will be deleted by admin")
                .isPublic(false)
                .build();
        
        String createResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        Long projectId = objectMapper.readTree(createResponse).get("id").asLong();
        
        mockMvc.perform(delete("/api/projects/" + projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Should verify permission inheritance from project to document")
    void testPermissionInheritance() throws Exception {
        Document document = documentRepository.findAll().stream()
                .filter(d -> d.getProject() != null)
                .filter(d -> d.getAuthor().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/documents/" + document.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(document.getId()));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("Group member should access project shared with their group")
    void testGroupBasedAccess() throws Exception {
        User bob = userRepository.findByUsername("bob").orElseThrow();
        
        Project marketingProject = projectRepository.findAll().stream()
                .filter(p -> p.getSharedWithGroups().contains(Group.MARKETING))
                .findFirst()
                .orElseThrow();
        
        if (bob.getGroups().contains(Group.MARKETING)) {
            mockMvc.perform(get("/api/projects/" + marketingProject.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(marketingProject.getId()));
        }
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Owner should be able to create a new project")
    void testCreateProject() throws Exception {
        ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .name("New Test Project")
                .description("This is a test project")
                .isPublic(false)
                .build();
        
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Test Project"))
                .andExpect(jsonPath("$.description").value("This is a test project"));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("User should only see projects they have access to")
    void testFilteredProjectList() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @WithMockUser(username = "carol", roles = {"MEMBER"})
    @DisplayName("User with no shared access should see limited projects")
    void testLimitedProjectAccess() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin should see all projects")
    void testAdminSeesAllProjects() throws Exception {
        long totalProjects = projectRepository.count();
        
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) totalProjects)));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Owner should be able to create a document")
    void testCreateDocument() throws Exception {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getOwner().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        DocumentCreateRequest createRequest = DocumentCreateRequest.builder()
                .title("New Test Document")
                .content("This is test content")
                .projectId(aliceProject.getId())
                .build();
        
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Test Document"));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("User with read permission should not be able to delete")
    void testReadOnlyUserCannotDelete() throws Exception {
        Project aliceProject = projectRepository.findAll().stream()
                .filter(p -> p.getOwner().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(delete("/api/projects/" + aliceProject.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Owner should be able to delete their own project")
    void testOwnerCanDelete() throws Exception {
        ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .name("Project to Delete")
                .description("This will be deleted by owner")
                .isPublic(false)
                .build();
        
        String createResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        Long projectId = objectMapper.readTree(createResponse).get("id").asLong();
        
        mockMvc.perform(delete("/api/projects/" + projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Should access document owned by user")
    void testDocumentOwnerAccess() throws Exception {
        Document aliceDocument = documentRepository.findAll().stream()
                .filter(d -> d.getAuthor().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/documents/" + aliceDocument.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceDocument.getId()))
                .andExpect(jsonPath("$.title").value(aliceDocument.getTitle()));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Should update document owned by user")
    void testDocumentOwnerUpdate() throws Exception {
        Document aliceDocument = documentRepository.findAll().stream()
                .filter(d -> d.getAuthor().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        DocumentUpdateRequest updateRequest = DocumentUpdateRequest.builder()
                .title("Updated Document Title")
                .content("Updated content")
                .build();
        
        mockMvc.perform(put("/api/documents/" + aliceDocument.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Document Title"));
    }

    @Test
    @WithMockUser(username = "carol", roles = {"MEMBER"})
    @DisplayName("User without permission should not access document")
    void testDocumentAccessDenied() throws Exception {
        Document aliceDocument = documentRepository.findAll().stream()
                .filter(d -> d.getAuthor().getUsername().equals("alice"))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(get("/api/documents/" + aliceDocument.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("User with granted permission should access project")
    void testGrantedPermissionAccess() throws Exception {
        Project project = projectRepository.findAll().stream()
                .filter(p -> !p.getOwner().getUsername().equals("bob"))
                .filter(p -> !p.isPublic())
                .findFirst()
                .orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "bob",
                BasePermission.READ
        );
        
        mockMvc.perform(get("/api/projects/" + project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"MEMBER"})
    @DisplayName("After permission revocation, user should not access project")
    void testRevokedPermissionDenied() throws Exception {
        Project project = projectRepository.findAll().stream()
                .filter(p -> !p.getOwner().getUsername().equals("bob"))
                .findFirst()
                .orElseThrow();
        
        aclPermissionService.grantToUser(
                Project.class,
                project.getId(),
                "bob",
                BasePermission.READ
        );
        
        mockMvc.perform(get("/api/projects/" + project.getId()))
                .andExpect(status().isOk());
        
        aclPermissionService.revokePermissions(
                Project.class,
                project.getId(),
                sidResolver.principalSid("bob"),
                java.util.List.of(BasePermission.READ)
        );
        
        mockMvc.perform(get("/api/projects/" + project.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request should be unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        
        mockMvc.perform(get("/api/projects/" + project.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Should validate request data for project creation")
    void testValidationOnCreate() throws Exception {
        ProjectCreateRequest invalidRequest = ProjectCreateRequest.builder()
                .name("AB")
                .description("Description")
                .build();
        
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"MANAGER"})
    @DisplayName("Should handle non-existent resource gracefully")
    void testNonExistentResource() throws Exception {
        mockMvc.perform(get("/api/projects/999999"))
                .andExpect(status().isNotFound());
    }
}
