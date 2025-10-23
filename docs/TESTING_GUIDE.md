# Testing Guide - Spring Boot ACL Demo

This guide covers testing strategies, test structure, and examples for the ACL demo application.

## Table of Contents

1. [Test Overview](#test-overview)
2. [Running Tests](#running-tests)
3. [Test Structure](#test-structure)
4. [Unit Testing](#unit-testing)
5. [Integration Testing](#integration-testing)
6. [ACL-Specific Testing](#acl-specific-testing)
7. [Testing Best Practices](#testing-best-practices)
8. [Test Data Setup](#test-data-setup)
9. [Common Test Scenarios](#common-test-scenarios)

---

## Test Overview

### Test Categories

The project includes several types of tests:

| Test Type | Purpose | Location |
|-----------|---------|----------|
| Unit Tests | Test individual components in isolation | `service/*UnitTests.java` |
| Integration Tests | Test component interactions with database | `service/*IntegrationTests.java` |
| REST API Tests | Test HTTP endpoints with security | `web/*IntegrationTests.java` |
| Infrastructure Tests | Verify ACL setup and configuration | `AclInfrastructureTests.java` |
| Negative Path Tests | Test error handling and edge cases | `service/AclNegativePathTests.java` |

### Test Coverage

```
src/test/java/com/example/acl/
├── AclDemoApplicationTests.java              # Application startup
├── AclInfrastructureTests.java               # ACL tables & setup
├── PermissionManagementControllerTests.java  # Permission API
├── service/
│   ├── AclServiceIntegrationTests.java       # ACL core operations
│   ├── AclPermissionServiceUnitTests.java    # Service unit tests
│   ├── AclGroupAndInheritanceTests.java      # Groups & inheritance
│   ├── AclCachingBehaviorTests.java          # Cache behavior
│   └── AclNegativePathTests.java             # Error scenarios
└── web/
    └── SecuredRestEndpointIntegrationTests.java  # REST API security
```

---

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=AclServiceIntegrationTests
```

### Run Specific Test Method

```bash
mvn test -Dtest=AclServiceIntegrationTests#testOwnerCanAccessOwnProject
```

### Run Tests with Coverage

```bash
mvn test jacoco:report
```

Coverage report available at: `target/site/jacoco/index.html`

### Run Tests in IDE

**IntelliJ IDEA:**
- Right-click on test class → Run
- Ctrl+Shift+F10 (Windows) / ⌃⇧R (Mac)

**VS Code:**
- Click on the "Run Test" button above the test method
- Use Testing panel

### Test Output Options

```bash
# Verbose output
mvn test -X

# Skip tests (not recommended)
mvn package -DskipTests

# Run only fast tests (exclude @Tag("slow"))
mvn test -Dgroups="!slow"
```

---

## Test Structure

### Standard Test Anatomy

```java
@SpringBootTest
@Transactional
class MyServiceTests {
    
    @Autowired
    private MyService myService;
    
    @Autowired
    private TestDataFactory testDataFactory;
    
    private Project testProject;
    
    @BeforeEach
    void setUp() {
        // Arrange: Set up test data
        testProject = testDataFactory.createProject("Test Project", "alice");
    }
    
    @Test
    void testSomething() {
        // Act: Perform the action
        Result result = myService.doSomething(testProject.getId());
        
        // Assert: Verify the outcome
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up if needed (usually handled by @Transactional)
    }
}
```

### Test Annotations

#### @SpringBootTest
Loads full application context:
```java
@SpringBootTest
class FullContextTests {
    // Full Spring context with all beans
}
```

#### @WebMvcTest
Tests only web layer:
```java
@WebMvcTest(ProjectController.class)
class ProjectControllerTests {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProjectService projectService;
}
```

#### @DataJpaTest
Tests only persistence layer:
```java
@DataJpaTest
class ProjectRepositoryTests {
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TestEntityManager entityManager;
}
```

#### @Transactional
Rolls back database changes after each test:
```java
@Transactional
@Test
void testDatabaseOperation() {
    // Changes rolled back automatically
}
```

#### @WithMockUser
Mocks authenticated user:
```java
@Test
@WithMockUser(username = "alice", roles = {"MANAGER"})
void testAsAlice() {
    // Test executes as user 'alice' with ROLE_MANAGER
}
```

---

## Unit Testing

### Service Unit Tests

Test business logic without database:

```java
@ExtendWith(MockitoExtension.class)
class AclPermissionServiceUnitTests {
    
    @Mock
    private MutableAclService aclService;
    
    @Mock
    private AclCache aclCache;
    
    @Mock
    private AclAuditService auditService;
    
    @InjectMocks
    private AclPermissionService aclPermissionService;
    
    @Test
    void testEnsureAclCreatesNewAclIfNotExists() {
        // Arrange
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, 1L);
        when(aclService.readAclById(oid))
            .thenThrow(new NotFoundException("Not found"));
        
        MutableAcl mockAcl = mock(MutableAcl.class);
        when(aclService.createAcl(oid)).thenReturn(mockAcl);
        
        // Act
        MutableAcl result = aclPermissionService.ensureAcl(Project.class, 1L);
        
        // Assert
        assertThat(result).isNotNull();
        verify(aclService).createAcl(oid);
        verify(auditService).publishChange(
            eq(AclAuditOperation.CREATE),
            eq(Project.class),
            eq(1L),
            isNull(),
            anyList(),
            anyString()
        );
    }
    
    @Test
    void testEnsureAclReturnsExistingAcl() {
        // Arrange
        ObjectIdentity oid = new ObjectIdentityImpl(Project.class, 1L);
        MutableAcl existingAcl = mock(MutableAcl.class);
        when(aclService.readAclById(oid)).thenReturn(existingAcl);
        
        // Act
        MutableAcl result = aclPermissionService.ensureAcl(Project.class, 1L);
        
        // Assert
        assertThat(result).isEqualTo(existingAcl);
        verify(aclService, never()).createAcl(any());
    }
}
```

### Testing with Mocks

```java
@Test
void testProjectCreationGrantsOwnership() {
    // Arrange
    when(userRepository.findByUsername("alice"))
        .thenReturn(Optional.of(testUser));
    when(projectRepository.save(any(Project.class)))
        .thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
    
    // Act
    Project project = projectService.createProject(createRequest);
    
    // Assert
    verify(aclPermissionService).applyOwnership(
        Project.class,
        1L,
        "alice"
    );
}
```

---

## Integration Testing

### Database Integration Tests

Test with real database interactions:

```java
@SpringBootTest
@Transactional
class AclServiceIntegrationTests {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private AclPermissionService aclPermissionService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Test
    @WithMockUser(username = "alice", roles = "MANAGER")
    void testCreateProjectAndVerifyAcl() {
        // Arrange
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Integration Test Project");
        request.setDescription("Test description");
        request.setIsPublic(false);
        
        // Act
        Project project = projectService.createProject(request);
        
        // Assert - Domain object
        assertThat(project).isNotNull();
        assertThat(project.getId()).isNotNull();
        assertThat(project.getName()).isEqualTo("Integration Test Project");
        assertThat(project.getOwner().getUsername()).isEqualTo("alice");
        
        // Assert - ACL created
        boolean hasPermission = aclPermissionService.hasPermission(
            SecurityContextHolder.getContext().getAuthentication(),
            Project.class,
            project.getId(),
            BasePermission.READ,
            BasePermission.WRITE,
            BasePermission.DELETE
        );
        assertThat(hasPermission).isTrue();
    }
    
    @Test
    @WithMockUser(username = "bob", roles = "MEMBER")
    void testNonOwnerCannotAccessPrivateProject() {
        // Arrange - Create project as alice
        Project aliceProject = createProjectAs("alice", false);
        
        // Act & Assert - Bob cannot access
        assertThatThrownBy(() -> 
            projectService.getProjectById(aliceProject.getId())
        ).isInstanceOf(AccessDeniedException.class);
    }
    
    private Project createProjectAs(String username, boolean isPublic) {
        return SecurityContextUtil.runAs(username, () -> {
            ProjectCreateRequest request = new ProjectCreateRequest();
            request.setName("Project by " + username);
            request.setIsPublic(isPublic);
            return projectService.createProject(request);
        });
    }
}
```

### REST API Integration Tests

Test HTTP endpoints with security:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class SecuredRestEndpointIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Test
    void testCreateProjectViaRest() {
        // Arrange
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("REST Test Project");
        request.setDescription("Created via REST");
        request.setIsPublic(false);
        
        HttpEntity<ProjectCreateRequest> entity = new HttpEntity<>(request);
        
        // Act
        ResponseEntity<ProjectResponse> response = restTemplate
            .withBasicAuth("alice", "password123")
            .postForEntity("/api/projects", entity, ProjectResponse.class);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("REST Test Project");
        assertThat(response.getBody().getOwnerUsername()).isEqualTo("alice");
    }
    
    @Test
    void testAccessDeniedReturns403() {
        // Arrange - Create project as alice
        Long projectId = createProjectAsAlice();
        
        // Act - Try to access as bob (no permission)
        ResponseEntity<ProjectResponse> response = restTemplate
            .withBasicAuth("bob", "password123")
            .getForEntity("/api/projects/" + projectId, ProjectResponse.class);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    
    @Test
    void testUnauthenticatedRequestReturns401() {
        // Act - No authentication
        ResponseEntity<ProjectResponse> response = restTemplate
            .getForEntity("/api/projects/1", ProjectResponse.class);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void testGrantPermissionViaRestApi() {
        // Arrange
        Long projectId = createProjectAsAlice();
        
        PermissionGrantRequest grantRequest = new PermissionGrantRequest();
        grantRequest.setResourceType("PROJECT");
        grantRequest.setResourceId(projectId);
        grantRequest.setSubjectType("USER");
        grantRequest.setSubjectIdentifier("bob");
        grantRequest.setPermissions(Arrays.asList("READ", "WRITE"));
        
        // Act - Grant as admin
        ResponseEntity<PermissionOperationResponse> grantResponse = restTemplate
            .withBasicAuth("admin", "admin123")
            .postForEntity("/api/permissions/grant", grantRequest, 
                PermissionOperationResponse.class);
        
        // Assert - Grant successful
        assertThat(grantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(grantResponse.getBody().isSuccess()).isTrue();
        
        // Assert - Bob can now access
        ResponseEntity<ProjectResponse> accessResponse = restTemplate
            .withBasicAuth("bob", "password123")
            .getForEntity("/api/projects/" + projectId, ProjectResponse.class);
        
        assertThat(accessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## ACL-Specific Testing

### Test Permission Inheritance

```java
@Test
@WithMockUser(username = "alice", roles = "MANAGER")
void testDocumentInheritsProjectPermissions() {
    // Arrange - Create project
    Project project = projectService.createProject(createProjectRequest("Test Project"));
    
    // Grant bob READ on project
    aclPermissionService.grantToUser(
        Project.class,
        project.getId(),
        "bob",
        BasePermission.READ
    );
    
    // Create document in project
    Document document = documentService.createDocument(
        createDocumentRequest("Test Doc", project.getId())
    );
    
    // Act - Check bob's permission on document (should inherit from project)
    boolean bobHasReadOnDoc = SecurityContextUtil.runAs("bob", () ->
        aclPermissionService.hasPermission(
            SecurityContextHolder.getContext().getAuthentication(),
            Document.class,
            document.getId(),
            BasePermission.READ
        )
    );
    
    // Assert
    assertThat(bobHasReadOnDoc).isTrue();
}
```

### Test Group Permissions

```java
@Test
void testGroupPermissionGrantsAccessToAllMembers() {
    // Arrange
    Project project = createProjectAsAlice();
    
    // Grant READ to ENGINEERING group
    aclPermissionService.grantToGroup(
        Project.class,
        project.getId(),
        Group.ENGINEERING,
        BasePermission.READ
    );
    
    // Act - Check access for group members
    boolean aliceHasAccess = checkAccessAs("alice", project.getId());  // ENGINEERING member
    boolean bobHasAccess = checkAccessAs("bob", project.getId());      // ENGINEERING member
    boolean carolHasAccess = checkAccessAs("carol", project.getId());  // MARKETING member
    
    // Assert
    assertThat(aliceHasAccess).isTrue();
    assertThat(bobHasAccess).isTrue();
    assertThat(carolHasAccess).isFalse();  // Not in ENGINEERING group
}
```

### Test Cache Behavior

```java
@Test
void testAclCacheEvictionOnUpdate() {
    // Arrange
    Project project = createProjectAsAlice();
    
    // Access once to populate cache
    aclPermissionService.hasPermission(
        getAuthenticationFor("alice"),
        Project.class,
        project.getId(),
        BasePermission.READ
    );
    
    // Act - Grant permission (should evict cache)
    aclPermissionService.grantToUser(
        Project.class,
        project.getId(),
        "bob",
        BasePermission.READ
    );
    
    // Assert - Bob can access (cache was evicted and reloaded)
    boolean bobHasAccess = aclPermissionService.hasPermission(
        getAuthenticationFor("bob"),
        Project.class,
        project.getId(),
        BasePermission.READ
    );
    
    assertThat(bobHasAccess).isTrue();
}
```

### Test Audit Logging

```java
@Test
void testAuditLogCapturesPermissionGrant() {
    // Arrange
    Project project = createProjectAsAlice();
    int initialLogCount = auditLogStore.findAll().size();
    
    // Act
    aclPermissionService.grantToUser(
        Project.class,
        project.getId(),
        "bob",
        BasePermission.READ,
        BasePermission.WRITE
    );
    
    // Assert
    List<AclAuditLogEntry> logs = auditLogStore.findAll();
    assertThat(logs).hasSize(initialLogCount + 1);
    
    AclAuditLogEntry latestLog = logs.get(logs.size() - 1);
    assertThat(latestLog.getOperation()).isEqualTo(AclAuditOperation.GRANT);
    assertThat(latestLog.getDomainClass()).isEqualTo(Project.class);
    assertThat(latestLog.getIdentifier()).isEqualTo(project.getId());
    assertThat(latestLog.getSid().toString()).contains("bob");
    assertThat(latestLog.getPermissions())
        .containsExactlyInAnyOrder(BasePermission.READ, BasePermission.WRITE);
}
```

---

## Testing Best Practices

### 1. Use Descriptive Test Names

```java
// Good
@Test
void testOwnerCanDeleteOwnProject() { }

@Test
void testNonOwnerCannotDeleteProjectWithoutDeletePermission() { }

// Bad
@Test
void testDelete() { }
```

### 2. Follow AAA Pattern

```java
@Test
void testExample() {
    // Arrange - Set up test data and preconditions
    Project project = createTestProject();
    
    // Act - Perform the action being tested
    boolean result = projectService.deleteProject(project.getId());
    
    // Assert - Verify the outcome
    assertThat(result).isTrue();
    assertThat(projectRepository.findById(project.getId())).isEmpty();
}
```

### 3. Test One Thing Per Test

```java
// Good - Separate tests
@Test
void testProjectCreationSetsOwner() { }

@Test
void testProjectCreationGrantsOwnerPermissions() { }

// Bad - Testing multiple things
@Test
void testProjectCreation() {
    // Tests owner, permissions, validation, etc.
}
```

### 4. Use Parameterized Tests for Multiple Scenarios

```java
@ParameterizedTest
@CsvSource({
    "alice, MANAGER, true",
    "bob, MEMBER, true",
    "dave, VIEWER, false"
})
void testUserAccessBasedOnRole(String username, String role, boolean shouldHaveAccess) {
    // Test implementation
}
```

### 5. Clean Up Test Data

```java
@AfterEach
void cleanUp() {
    // Clean up if @Transactional is not used
    projectRepository.deleteAll();
}
```

### 6. Use Test Utilities

```java
// Create reusable test helpers
public class TestDataFactory {
    
    public static Project createProject(String name, String owner) {
        return Project.builder()
            .name(name)
            .owner(createUser(owner))
            .isPublic(false)
            .build();
    }
    
    public static User createUser(String username) {
        return User.builder()
            .username(username)
            .email(username + "@example.com")
            .password("password")
            .role(Role.MEMBER)
            .build();
    }
}
```

---

## Test Data Setup

### Using @DataJpaTest

```java
@DataJpaTest
class ProjectRepositoryTests {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Test
    void testFindByOwner() {
        // Arrange
        User alice = entityManager.persist(createUser("alice"));
        Project project = entityManager.persist(createProject("Test", alice));
        entityManager.flush();
        
        // Act
        List<Project> result = projectRepository.findByOwner(alice);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test");
    }
}
```

### Using @Sql Scripts

```java
@SpringBootTest
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DataDrivenTests {
    // Test data loaded from SQL scripts
}
```

---

## Common Test Scenarios

### Scenario 1: Owner Access

```java
@Test
@WithMockUser(username = "alice", roles = "MANAGER")
void testOwnerCanAccessOwnProject() {
    Project project = projectService.createProject(createRequest);
    
    assertThatCode(() -> 
        projectService.getProjectById(project.getId())
    ).doesNotThrowAnyException();
}
```

### Scenario 2: Access Denied

```java
@Test
@WithMockUser(username = "bob", roles = "MEMBER")
void testNonOwnerCannotAccessPrivateProject() {
    Long projectId = createProjectAsUser("alice");
    
    assertThatThrownBy(() -> 
        projectService.getProjectById(projectId)
    ).isInstanceOf(AccessDeniedException.class);
}
```

### Scenario 3: Public Access

```java
@Test
void testAnyAuthenticatedUserCanAccessPublicProject() {
    Project publicProject = createPublicProject();
    
    for (String user : Arrays.asList("alice", "bob", "carol", "dave")) {
        assertThatCode(() -> 
            SecurityContextUtil.runAs(user, () -> 
                projectService.getProjectById(publicProject.getId())
            )
        ).doesNotThrowAnyException();
    }
}
```

### Scenario 4: Shared Access

```java
@Test
void testSharedUserCanAccess() {
    // Alice creates project
    Project project = SecurityContextUtil.runAs("alice", () -> 
        projectService.createProject(createRequest)
    );
    
    // Grant bob READ permission
    aclPermissionService.grantToUser(
        Project.class, project.getId(), "bob", BasePermission.READ
    );
    
    // Bob can read
    assertThatCode(() -> 
        SecurityContextUtil.runAs("bob", () -> 
            projectService.getProjectById(project.getId())
        )
    ).doesNotThrowAnyException();
    
    // Bob cannot write (no WRITE permission)
    assertThatThrownBy(() -> 
        SecurityContextUtil.runAs("bob", () -> 
            projectService.updateProject(project.getId(), updateRequest)
        )
    ).isInstanceOf(AccessDeniedException.class);
}
```

### Scenario 5: Admin Override

```java
@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void testAdminCanAccessAnyProject() {
    Project alicePrivateProject = createProjectAsUser("alice");
    
    // Admin can access despite not being owner
    assertThatCode(() -> 
        projectService.getProjectById(alicePrivateProject.getId())
    ).doesNotThrowAnyException();
}
```

---

## Running Specific Test Suites

### Unit Tests Only

```bash
mvn test -Dtest="**/*UnitTests"
```

### Integration Tests Only

```bash
mvn test -Dtest="**/*IntegrationTests"
```

### REST API Tests

```bash
mvn test -Dtest="**/web/*Tests"
```

### Fast Tests (exclude slow tests)

```java
@Test
@Tag("slow")
void slowTest() { }

// Run: mvn test -Dgroups="!slow"
```

---

## Continuous Integration

### GitHub Actions Example

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'
      - name: Run tests
        run: mvn test
      - name: Generate coverage
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

---

## Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Spring Security Testing](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
