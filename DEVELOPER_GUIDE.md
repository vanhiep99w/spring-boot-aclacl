# Spring Boot ACL Demo - Developer Guide

## Table of Contents

1. [Introduction](#introduction)
2. [System Architecture](#system-architecture)
3. [Getting Started](#getting-started)
4. [ACL Concepts](#acl-concepts)
5. [Implementation Details](#implementation-details)
6. [API Reference](#api-reference)
7. [Extending the System](#extending-the-system)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Topics](#advanced-topics)

---

## Introduction

### What is This Project?

This Spring Boot ACL Demo is a comprehensive example of implementing **fine-grained, object-level authorization** using Spring Security Access Control Lists (ACL). It demonstrates how to secure REST APIs with permissions that can be controlled at the individual resource level rather than just role-based access.

### Key Features

- ✅ **Object-Level Security**: Each Project, Document, and Comment can have unique permissions
- ✅ **ACL Inheritance**: Permissions cascade from Projects → Documents → Comments
- ✅ **Multiple Subject Types**: Grant permissions to users, roles, or groups
- ✅ **Custom Permissions**: Beyond READ/WRITE, includes SHARE and APPROVE permissions
- ✅ **Performance Optimized**: EhCache integration for ACL lookups
- ✅ **Audit Trail**: Complete audit log of all permission changes
- ✅ **Discovery API**: Find what resources a user can access
- ✅ **REST API**: Full CRUD operations with ACL enforcement

### Use Cases

This demo is ideal for understanding ACL concepts in:
- Document management systems
- Project collaboration platforms
- Multi-tenant applications
- Content management systems
- Any application requiring per-resource permissions

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                          │
│  (Controllers with @PreAuthorize/@PostAuthorize)            │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│              Service Layer                                   │
│  (Business Logic + ACL Permission Management)               │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│         Spring Security ACL Infrastructure                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  ACL Cache   │  │  ACL Service │  │ Lookup       │     │
│  │  (EhCache)   │  │  (JDBC)      │  │ Strategy     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│              Database Layer (H2)                            │
│  ACL Tables:                    Domain Tables:              │
│  - acl_sid                      - users                     │
│  - acl_class                    - projects                  │
│  - acl_object_identity          - documents                 │
│  - acl_entry                    - comments                  │
└─────────────────────────────────────────────────────────────┘
```

### Component Breakdown

#### 1. REST API Layer
- **Controllers**: `ProjectController`, `DocumentController`, `CommentController`, `PermissionManagementController`
- **Security Annotations**: `@PreAuthorize`, `@PostAuthorize`, `@PostFilter`
- **DTOs & Mappers**: Clean separation between domain and API layers

#### 2. Service Layer
- **Business Services**: `ProjectService`, `DocumentService`, `CommentService`
- **ACL Services**: `AclPermissionService`, `PermissionDiscoveryService`
- **Audit Service**: `AclAuditService` for tracking permission changes
- **SID Resolver**: `AclSidResolver` for converting users/groups/roles to SIDs

#### 3. ACL Infrastructure
- **MutableAclService**: JDBC-based ACL management
- **AclCache**: EhCache for performance optimization
- **MethodSecurityExpressionHandler**: Custom expressions for domain-specific checks
- **Permission Registry**: Manages standard and custom permissions

#### 4. Database Layer
- **ACL Schema**: Four tables for ACL data
- **Domain Schema**: Entity tables for Projects, Documents, Comments, Users
- **H2 Database**: In-memory database for easy development

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- (Optional) IDE with Spring Boot support (IntelliJ IDEA, VS Code, Eclipse)

### Quick Start

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd spring-boot-acl-demo
   mvn clean install
   ```

2. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```
   
   The application starts on `http://localhost:8080`

3. **Verify ACL Setup**
   ```bash
   curl -u admin:admin123 http://localhost:8080/api/acl/status
   ```
   
   Expected response:
   ```json
   {
     "aclSids": 10,
     "aclClasses": 3,
     "aclObjectIdentities": 14,
     "aclEntries": 70,
     "status": "ACL infrastructure is operational"
   }
   ```

4. **Test Basic Operations**
   ```bash
   # List all projects (alice can see)
   curl -u alice:password123 http://localhost:8080/api/projects
   
   # Create a new project
   curl -X POST http://localhost:8080/api/projects \
     -u alice:password123 \
     -H "Content-Type: application/json" \
     -d '{"name": "My Project", "description": "Test project", "isPublic": false}'
   ```

### Default Test Users

| Username | Password    | Role    | Groups              |
|----------|-------------|---------|---------------------|
| admin    | admin123    | ADMIN   | EXECUTIVE, ENGINEERING |
| alice    | password123 | MANAGER | ENGINEERING         |
| bob      | password123 | MEMBER  | ENGINEERING         |
| carol    | password123 | MEMBER  | MARKETING           |
| dave     | password123 | VIEWER  | SALES               |

### Development Tools

- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:acldb`
  - Username: `sa`
  - Password: (empty)

- **Actuator Health**: http://localhost:8080/actuator/health

---

## ACL Concepts

### What are Access Control Lists?

Access Control Lists (ACLs) provide **object-level** security by maintaining a list of permissions for each individual resource. Unlike role-based access control (RBAC) which grants permissions based on a user's role, ACLs allow you to specify exactly which users can do what with each specific object.

### Core ACL Components

#### 1. Object Identity (OID)
Uniquely identifies a secured domain object:
```java
ObjectIdentity oid = new ObjectIdentityImpl(Project.class, projectId);
```

#### 2. Security Identity (SID)
Represents a subject (user, role, or group):
```java
PrincipalSid userSid = new PrincipalSid("alice");
GrantedAuthoritySid roleSid = new GrantedAuthoritySid("ROLE_ADMIN");
GrantedAuthoritySid groupSid = new GrantedAuthoritySid("GROUP_ENGINEERING");
```

#### 3. Permissions
Define what actions can be performed:
- **READ** (1): View the resource
- **WRITE** (2): Modify the resource
- **CREATE** (4): Create child resources
- **DELETE** (8): Delete the resource
- **ADMINISTRATION** (16): Full control including permission management

**Custom Permissions:**
- **SHARE** (32): Share the resource with others
- **APPROVE** (64): Approve changes to the resource

#### 4. ACL Entry
Links a SID to an Object Identity with specific permissions:
```
| Object Identity    | SID      | Permissions           |
|--------------------|----------|-----------------------|
| Project:1         | alice    | READ, WRITE, DELETE   |
| Project:1         | bob      | READ                  |
| Project:1         | GROUP_ENG| READ, WRITE           |
```

### ACL Database Schema

#### acl_sid
Stores security identities (users, roles, groups):
```sql
id | principal | sid
1  | true      | alice
2  | true      | bob
3  | false     | ROLE_ADMIN
4  | false     | GROUP_ENGINEERING
```

#### acl_class
Stores domain class names:
```sql
id | class
1  | com.example.acl.domain.Project
2  | com.example.acl.domain.Document
3  | com.example.acl.domain.Comment
```

#### acl_object_identity
Stores object instances with ownership and inheritance:
```sql
id | object_id_class | object_id_identity | parent_object | owner_sid | entries_inheriting
1  | 1               | 1                  | NULL          | 1         | true
2  | 2               | 1                  | 1             | 1         | true
```

#### acl_entry
Stores permission grants:
```sql
id | acl_object_identity | ace_order | sid | mask | granting
1  | 1                   | 0         | 1   | 31   | true
2  | 1                   | 1         | 2   | 1    | true
```

### Permission Inheritance

One of the most powerful ACL features is **inheritance**, allowing child objects to inherit permissions from parent objects:

```
Project (id: 1)
  └─ owner: alice
  └─ permissions: alice (ALL), bob (READ)
     │
     └─ Document (id: 1)
        └─ author: alice
        └─ inherits from Project
        └─ effective permissions: alice (ALL), bob (READ)
           │
           └─ Comment (id: 1)
              └─ author: bob
              └─ inherits from Document
              └─ effective permissions: alice (ALL), bob (READ)
```

**Inheritance Flow:**
1. When checking permissions on a Document, ACL checks the Document's ACL first
2. If `entriesInheriting = true`, it also checks the parent Project's ACL
3. The permission is granted if found in either the Document or Project ACL
4. This cascades down to Comments as well

**Benefits:**
- Grant READ on a Project → automatically grants READ on all Documents in that Project
- Simplifies permission management for hierarchical data
- Reduces ACL entries needed in the database

### ACL Caching

To improve performance, ACL lookups are cached using EhCache:

```java
@Bean
public AclCache aclCache() {
    return new EhCacheBasedAclCache(
        ehCacheAcl().getObject(),
        permissionGrantingStrategy(),
        aclAuthorizationStrategy()
    );
}
```

**Cache Configuration:**
- **TTL**: 900 seconds (15 minutes)
- **Idle Time**: 300 seconds (5 minutes)
- **Max Entries**: 2048
- **Eviction Policy**: LRU (Least Recently Used)

**Cache Behavior:**
- ACL lookups first check the cache
- On cache miss, loads from database and caches the result
- Automatically evicted when ACLs are updated
- Manually evicted via `AclPermissionService.evictCache()`

### Permission Evaluation

When a secured method is called:

1. **Security Annotation Triggers**: `@PreAuthorize("hasPermission(#id, 'Project', 'READ')")`
2. **Expression Handler**: Parses the security expression
3. **Permission Evaluator**: Looks up the ACL for the resource
4. **SID Retrieval**: Gets all SIDs for the current user (username, roles, groups)
5. **ACL Check**: Checks if any of the user's SIDs have the required permission
6. **Inheritance Check**: If enabled, checks parent ACLs
7. **Decision**: Grants or denies access

---

## Implementation Details

### Security Configuration

#### Basic Authentication
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/h2-console/**", "/actuator/health").permitAll()
            .anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable());
    return http.build();
}
```

#### Method Security
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // Enables @PreAuthorize, @PostAuthorize, @PostFilter
}
```

### ACL Configuration

See `src/main/java/com/example/acl/config/AclConfig.java` for complete configuration.

Key beans:
- `AclAuthorizationStrategy`: Controls who can modify ACLs (ADMIN role)
- `PermissionGrantingStrategy`: Determines if permissions are granted
- `MutableAclService`: JDBC-based ACL management
- `MethodSecurityExpressionHandler`: Enables `hasPermission()` in security expressions

### Automatic Owner Assignment

When a resource is created, the creator automatically becomes the owner with full permissions:

```java
@Service
public class ProjectService {
    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        User currentUser = getCurrentUser();
        
        // Create domain object
        Project project = Project.builder()
            .name(request.getName())
            .owner(currentUser)
            .build();
        project = projectRepository.save(project);
        
        // Apply ACL ownership (grants all permissions)
        aclPermissionService.applyOwnership(
            Project.class, 
            project.getId(), 
            currentUser.getUsername()
        );
        
        return project;
    }
}
```

### Controller Security Annotations

#### @PreAuthorize (Check Before Execution)
```java
@PreAuthorize("hasPermission(#id, 'com.example.acl.domain.Project', 'WRITE')")
public ResponseEntity<ProjectResponse> updateProject(
    @PathVariable Long id,
    @RequestBody ProjectUpdateRequest request) {
    // Only executed if user has WRITE permission on Project with id
}
```

#### @PostAuthorize (Check After Execution)
```java
@PostAuthorize("hasPermission(returnObject.body, 'READ')")
public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
    // Returns 403 if user doesn't have READ permission on returned object
}
```

#### @PostFilter (Filter Collection Results)
```java
@PostFilter("hasPermission(filterObject, 'READ')")
public ResponseEntity<List<Project>> getAllProjects() {
    // Returns only projects the user has READ permission for
}
```

### Custom Security Expressions

Beyond standard `hasPermission()`, the system provides custom expressions:

```java
@PreAuthorize("@customSecurityExpressions.isDocumentOwner(#id)")
public void deleteDocument(Long id) {
    // Only document owner can delete
}

@PreAuthorize("@customSecurityExpressions.hasProjectRole(#projectId, 'MANAGER')")
public void updateProjectSettings(Long projectId, Settings settings) {
    // Only users with MANAGER role on the project
}
```

### Auditing

All ACL changes are automatically audited:

```java
@Service
public class AclAuditService {
    public void publishChange(
        AclAuditOperation operation,  // GRANT, REVOKE, OWNERSHIP, INHERITANCE
        Class<?> domainClass,
        Serializable identifier,
        Sid sid,
        List<Permission> permissions,
        String actor
    ) {
        AclAuditLogEntry entry = new AclAuditLogEntry(
            operation, domainClass, identifier, sid, permissions, actor
        );
        auditLogStore.save(entry);
        applicationEventPublisher.publishEvent(
            new AclPermissionChangeEvent(this, entry)
        );
    }
}
```

Audit logs can be queried:
```java
List<AclAuditLogEntry> logs = auditLogStore.findAll();
List<AclAuditLogEntry> projectLogs = auditLogStore.findByResource(
    Project.class, projectId
);
```

---

## API Reference

### REST Endpoints Overview

| Endpoint Pattern | Resource | Description |
|-----------------|----------|-------------|
| `/api/projects` | Project | CRUD operations for projects |
| `/api/documents` | Document | CRUD operations for documents |
| `/api/comments` | Comment | CRUD operations for comments |
| `/api/permissions` | Permission Management | Grant, revoke, query permissions |
| `/api/acl/status` | ACL Diagnostics | Check ACL infrastructure status |

### Detailed API Documentation

For complete API documentation with examples, see:
- [API Examples](docs/API_EXAMPLES.md) - Complete curl examples
- [Permission API](docs/PERMISSION_API.md) - Permission management endpoints
- [OpenAPI Specification](docs/OPENAPI_SPEC.yaml) - Swagger/OpenAPI spec

### Quick API Examples

#### Create a Project
```bash
curl -X POST http://localhost:8080/api/projects \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Project",
    "description": "A new project",
    "isPublic": false
  }'
```

#### Grant Permissions
```bash
curl -X POST http://localhost:8080/api/permissions/grant \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceId": 1,
    "subjectType": "USER",
    "subjectIdentifier": "bob",
    "permissions": ["READ", "WRITE"]
  }'
```

#### Check User Permissions
```bash
curl -X GET "http://localhost:8080/api/permissions/check?resourceType=PROJECT&resourceId=1" \
  -u bob:password123
```

#### List Accessible Resources
```bash
curl -X GET "http://localhost:8080/api/permissions/accessible?resourceType=PROJECT" \
  -u bob:password123
```

---

## Extending the System

### Adding Custom Permissions

1. **Define the Permission**

Create a new permission class:

```java
public class CustomAclPermission extends BasePermission {
    public static final Permission SHARE = new CustomAclPermission(32, 'S');
    public static final Permission APPROVE = new CustomAclPermission(64, 'A');
    public static final Permission EXPORT = new CustomAclPermission(128, 'X');
    
    protected CustomAclPermission(int mask, char code) {
        super(mask, code);
    }
}
```

2. **Register in Permission Registry**

```java
@Component
public class AclPermissionRegistry extends DefaultPermissionFactory {
    
    public AclPermissionRegistry() {
        super();
        registerPublicPermissions(CustomAclPermission.class);
    }
    
    @Override
    public Permission buildFromName(String name) {
        return switch (name.toUpperCase()) {
            case "SHARE" -> CustomAclPermission.SHARE;
            case "APPROVE" -> CustomAclPermission.APPROVE;
            case "EXPORT" -> CustomAclPermission.EXPORT;
            default -> super.buildFromName(name);
        };
    }
}
```

3. **Use in Security Annotations**

```java
@PreAuthorize("hasPermission(#id, 'com.example.acl.domain.Document', 'EXPORT')")
public byte[] exportDocument(@PathVariable Long id) {
    // Only users with EXPORT permission can export
}
```

4. **Grant the Permission**

```java
aclPermissionService.grantToUser(
    Document.class, 
    documentId, 
    "alice", 
    CustomAclPermission.EXPORT
);
```

### Adding New Domain Entities

1. **Create the Entity**

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    
    @ManyToOne
    private User assignee;
    
    @ManyToOne
    private Project project;
}
```

2. **Create Service with ACL Support**

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final AclPermissionService aclPermissionService;
    
    @Transactional
    public Task createTask(TaskCreateRequest request) {
        User currentUser = getCurrentUser();
        
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setAssignee(currentUser);
        task = taskRepository.save(task);
        
        // Apply ACL ownership
        aclPermissionService.applyOwnership(
            Task.class,
            task.getId(),
            currentUser.getUsername()
        );
        
        // Optionally set up inheritance
        if (request.getProjectId() != null) {
            aclPermissionService.setParent(
                Task.class, task.getId(),
                Project.class, request.getProjectId(),
                true  // entries inheriting
            );
        }
        
        return task;
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#id, 'com.example.acl.domain.Task', 'READ')")
    public Task getTask(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }
}
```

3. **Create Controller**

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskCreateRequest request) {
        Task task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        Task task = taskService.getTask(id);
        return ResponseEntity.ok(task);
    }
}
```

### Adding Permission Discovery

To help users discover what permissions they have:

```java
@Service
public class PermissionDiscoveryService {
    
    public List<ResourceAccessInfo> findAccessibleResources(
        Class<?> domainClass,
        String username
    ) {
        List<Sid> sids = sidRetrievalStrategy.getSids(
            SecurityContextHolder.getContext().getAuthentication()
        );
        
        // Query ACL entries for this user's SIDs
        List<AclObjectIdentity> objectIdentities = 
            jdbcTemplate.query(/* query all objects with ACL entries for SIDs */);
        
        return objectIdentities.stream()
            .map(oid -> new ResourceAccessInfo(
                oid.getObjectId(),
                getResourceName(domainClass, oid.getObjectId()),
                getPermissions(oid, sids),
                determineAccessSource(oid, sids)
            ))
            .collect(Collectors.toList());
    }
}
```

### Custom Security Expressions

Add domain-specific security checks:

```java
@Component("taskSecurity")
public class TaskSecurityExpressions {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public boolean isAssignee(Long taskId) {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return taskRepository.findById(taskId)
            .map(task -> task.getAssignee().getUsername().equals(username))
            .orElse(false);
    }
    
    public boolean canReassign(Long taskId) {
        Authentication auth = SecurityContextHolder.getContext()
            .getAuthentication();
        // Custom logic: only ADMIN or project owner can reassign
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
            || isProjectOwner(taskId);
    }
}
```

Use in controllers:
```java
@PreAuthorize("@taskSecurity.isAssignee(#id) or @taskSecurity.canReassign(#id)")
public void reassignTask(Long id, Long newAssigneeId) {
    // ...
}
```

---

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AclServiceIntegrationTests

# Run tests with coverage
mvn test jacoco:report
```

### Test Structure

```
src/test/java/com/example/acl/
├── AclDemoApplicationTests.java              # Context loading
├── AclInfrastructureTests.java               # ACL setup verification
├── PermissionManagementControllerTests.java  # Permission API tests
├── service/
│   ├── AclServiceIntegrationTests.java       # Core ACL operations
│   ├── AclPermissionServiceUnitTests.java    # Unit tests
│   ├── AclGroupAndInheritanceTests.java      # Group/inheritance tests
│   ├── AclCachingBehaviorTests.java          # Cache tests
│   └── AclNegativePathTests.java             # Error scenarios
└── web/
    └── SecuredRestEndpointIntegrationTests.java  # REST API security tests
```

### Writing ACL Tests

#### Unit Test Example

```java
@SpringBootTest
@WithMockUser(username = "alice", roles = "MANAGER")
class ProjectServiceTests {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private AclPermissionService aclPermissionService;
    
    @Test
    void testOwnerCanAccessProject() {
        // Given
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Test Project");
        
        // When
        Project project = projectService.createProject(request);
        
        // Then
        assertThat(project).isNotNull();
        assertThat(project.getOwner().getUsername()).isEqualTo("alice");
        
        // Verify ACL
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
        // Given a project owned by alice
        Long projectId = createProjectAsAlice();
        
        // When/Then
        assertThatThrownBy(() -> projectService.getProjectById(projectId))
            .isInstanceOf(AccessDeniedException.class);
    }
}
```

#### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class SecuredRestEndpointIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCreateAndAccessProject() {
        // Create project as alice
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Integration Test Project");
        
        ResponseEntity<ProjectResponse> createResponse = restTemplate
            .withBasicAuth("alice", "password123")
            .postForEntity("/api/projects", request, ProjectResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long projectId = createResponse.getBody().getId();
        
        // Alice can access
        ResponseEntity<ProjectResponse> aliceResponse = restTemplate
            .withBasicAuth("alice", "password123")
            .getForEntity("/api/projects/" + projectId, ProjectResponse.class);
        
        assertThat(aliceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Bob cannot access (no permission)
        ResponseEntity<ProjectResponse> bobResponse = restTemplate
            .withBasicAuth("bob", "password123")
            .getForEntity("/api/projects/" + projectId, ProjectResponse.class);
        
        assertThat(bobResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

For complete testing guide, see [TESTING_GUIDE.md](docs/TESTING_GUIDE.md).

---

## Troubleshooting

### Common Issues

#### 1. "Access is Denied" Error

**Symptoms:**
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

**Possible Causes:**
- User doesn't have required permission on the resource
- ACL entry missing in database
- Cache not updated after permission grant

**Solutions:**

1. Check ACL entries:
```sql
-- Find ACL for specific resource
SELECT * FROM acl_object_identity 
WHERE object_id_identity = '1' 
AND object_id_class = (SELECT id FROM acl_class WHERE class = 'com.example.acl.domain.Project');

-- Find permission entries
SELECT e.*, s.sid, s.principal 
FROM acl_entry e 
JOIN acl_sid s ON e.sid = s.id 
WHERE e.acl_object_identity = <object_id>;
```

2. Grant missing permission:
```java
aclPermissionService.grantToUser(Project.class, projectId, "username", BasePermission.READ);
```

3. Clear cache:
```java
aclPermissionService.evictCache(Project.class, projectId);
```

#### 2. ACL Tables Not Created

**Symptoms:**
- Application fails to start
- "Table 'acl_sid' doesn't exist" error

**Solutions:**

1. Verify `application.properties`:
```properties
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

2. Check `schema.sql` is in `src/main/resources/`

3. Enable SQL logging:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

#### 3. Inheritance Not Working

**Symptoms:**
- User has permission on parent but can't access child
- Permission checks fail for inherited objects

**Solutions:**

1. Verify inheritance is set up:
```java
aclPermissionService.setParent(
    Document.class, documentId,
    Project.class, projectId,
    true  // MUST be true for entries inheriting
);
```

2. Check in database:
```sql
SELECT * FROM acl_object_identity WHERE id = <child_id>;
-- parent_object should be set
-- entries_inheriting should be true
```

3. Verify parent ACL exists:
```sql
SELECT * FROM acl_object_identity WHERE id = <parent_id>;
```

#### 4. Cache Stale Data

**Symptoms:**
- Permission granted but still getting access denied
- Changes not reflected until restart

**Solutions:**

1. Manual cache eviction:
```java
aclPermissionService.evictCache(domainClass, identifier);
```

2. Automatic eviction on updates (already implemented in `AclPermissionService`)

3. Reduce cache TTL in development:
```java
@Bean
public EhCacheFactoryBean ehCacheAcl() {
    CacheConfiguration config = new CacheConfiguration()
        .timeToLiveSeconds(60)  // Shorter TTL for development
        .timeToIdleSeconds(30);
    // ...
}
```

#### 5. Performance Issues

**Symptoms:**
- Slow permission checks
- High database load

**Solutions:**

1. Enable and verify caching is working:
```properties
logging.level.org.springframework.security.acls=DEBUG
```

2. Increase cache size:
```java
.maxEntriesLocalHeap(5000)  // Increase from default 2048
```

3. Use batch operations:
```java
// Instead of multiple individual grants
for (Long id : projectIds) {
    aclPermissionService.grantToUser(...);  // Slow
}

// Use bulk operation
aclPermissionService.bulkGrantToUsers(
    Project.class, projectIds, "username", BasePermission.READ
);
```

4. Optimize queries with indexes (already in `schema.sql`)

For more troubleshooting scenarios, see [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

---

## Advanced Topics

### Multi-Tenancy with ACL

Implement tenant isolation using ACL:

```java
@Service
public class TenantAwareProjectService {
    
    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        
        Project project = projectRepository.save(/* ... */);
        
        // Grant all permissions to tenant's admin group
        aclPermissionService.grantToGroup(
            Project.class,
            project.getId(),
            Group.valueOf("TENANT_" + currentTenant.getId() + "_ADMIN"),
            permissionRegistry.ownerDefaults().toArray(new Permission[0])
        );
        
        return project;
    }
}
```

### Dynamic Permission Evaluation

Create permissions based on business rules:

```java
@Component
public class DynamicPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(
        Authentication authentication,
        Object targetDomainObject,
        Object permission
    ) {
        if (targetDomainObject instanceof Document doc) {
            // Custom rule: draft documents can only be read by author
            if (doc.getStatus() == DocumentStatus.DRAFT) {
                return doc.getAuthor().getUsername()
                    .equals(authentication.getName());
            }
        }
        
        // Fall back to ACL
        return aclPermissionEvaluator.hasPermission(
            authentication, targetDomainObject, permission
        );
    }
}
```

### Audit Log Querying

Build admin dashboards with audit data:

```java
@Service
public class AuditReportService {
    
    public AuditReport generateReport(LocalDateTime from, LocalDateTime to) {
        List<AclAuditLogEntry> logs = auditLogStore.findByDateRange(from, to);
        
        return AuditReport.builder()
            .totalChanges(logs.size())
            .grantCount(countByOperation(logs, AclAuditOperation.GRANT))
            .revokeCount(countByOperation(logs, AclAuditOperation.REVOKE))
            .topActors(findTopActors(logs))
            .topResources(findTopResources(logs))
            .build();
    }
}
```

### Performance Optimization

#### 1. Batch Permission Checks

```java
public Map<Long, Boolean> checkBatchPermissions(
    List<Long> projectIds,
    Permission permission
) {
    Authentication auth = SecurityContextHolder.getContext()
        .getAuthentication();
    List<Sid> sids = sidRetrievalStrategy.getSids(auth);
    
    // Single database query for all projects
    List<ObjectIdentity> oids = projectIds.stream()
        .map(id -> new ObjectIdentityImpl(Project.class, id))
        .collect(Collectors.toList());
    
    Map<ObjectIdentity, Acl> acls = aclService.readAclsById(oids, sids);
    
    // Check permissions in memory
    return projectIds.stream()
        .collect(Collectors.toMap(
            id -> id,
            id -> {
                Acl acl = acls.get(new ObjectIdentityImpl(Project.class, id));
                return acl != null && acl.isGranted(
                    List.of(permission), sids, false
                );
            }
        ));
}
```

#### 2. Eager Loading with ACL Filtering

```java
@Query("SELECT p FROM Project p " +
       "LEFT JOIN FETCH p.owner " +
       "LEFT JOIN FETCH p.documents " +
       "WHERE p.id IN :accessibleIds")
List<Project> findAccessibleProjectsWithDocuments(@Param("accessibleIds") List<Long> ids);
```

### Webhook Integration

Notify external systems of permission changes:

```java
@Component
public class AclWebhookListener {
    
    @Async
    @EventListener
    public void onPermissionChange(AclPermissionChangeEvent event) {
        AclAuditLogEntry entry = event.getLogEntry();
        
        WebhookPayload payload = WebhookPayload.builder()
            .event("permission.changed")
            .operation(entry.getOperation().toString())
            .resourceType(entry.getDomainClass().getSimpleName())
            .resourceId(entry.getIdentifier().toString())
            .subject(entry.getSid().toString())
            .actor(entry.getActor())
            .timestamp(entry.getTimestamp())
            .build();
        
        webhookService.send(payload);
    }
}
```

---

## Additional Resources

### Documentation Files

- [README.md](README.md) - Project overview and quick start
- [API Examples](docs/API_EXAMPLES.md) - Complete REST API examples
- [Permission API](docs/PERMISSION_API.md) - Permission management API
- [ACL Setup Guide](docs/ACL_SETUP.md) - Infrastructure setup
- [Domain Model](docs/DOMAIN_MODEL.md) - Entity relationships
- [Testing Guide](docs/TESTING_GUIDE.md) - Comprehensive testing guide
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions

### External Resources

- [Spring Security ACL Documentation](https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Domain Object Security](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html#el-access-web-beans)

---

## Contributing

When contributing to this project:

1. Follow existing code style and conventions
2. Add tests for new functionality
3. Update documentation
4. Ensure all tests pass
5. Add audit logging for ACL changes
6. Consider performance implications

## License

This project is provided as-is for educational and demonstration purposes.

---

**Version:** 1.0.0  
**Last Updated:** 2024-01-15  
**Maintained by:** Spring Boot ACL Demo Team
