# Spring Boot ACL Demo

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Overview

This project is a **comprehensive, production-ready example** of implementing **Spring Security Access Control Lists (ACL)** for fine-grained, object-level authorization in a Spring Boot 3.x application. It demonstrates how to secure REST APIs with permissions that can be controlled at the individual resource level, going beyond simple role-based access control.

### What Makes This Demo Unique?

- ✅ **Complete Implementation**: Full CRUD operations with ACL enforcement
- ✅ **Permission Inheritance**: Documents inherit from Projects, Comments from Documents
- ✅ **Multiple Subject Types**: Grant permissions to users, roles, or groups
- ✅ **Custom Permissions**: Beyond READ/WRITE, includes SHARE and APPROVE
- ✅ **Performance Optimized**: EhCache integration for ACL lookups
- ✅ **Audit Trail**: Complete audit log of all permission changes
- ✅ **Discovery API**: Find what resources a user can access
- ✅ **Comprehensive Documentation**: Developer guide, API specs, and diagrams

### Use Cases

Perfect for understanding ACL in:
- 📁 Document management systems
- 👥 Project collaboration platforms
- 🏢 Multi-tenant applications
- 📝 Content management systems
- 🔒 Any application requiring per-resource permissions

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.3.4 | Application framework |
| Spring Security | 6.x | Security & ACL |
| Spring Data JPA | 3.x | Data persistence |
| H2 Database | 2.x | In-memory database |
| Jakarta Validation | 3.x | Input validation |
| Lombok | 1.18.x | Boilerplate reduction |
| EhCache | 2.10.x | ACL caching |
| JUnit 5 | 5.x | Testing framework |

## Architecture

The application is structured using the `com.example.acl` base package with the following sub-packages:

- **`config`** – Security, ACL, and application configuration
- **`domain`** – Entity classes (Project, Document, Comment, User)
- **`repository`** – Spring Data JPA repositories
- **`service`** – Business logic and ACL permission management
- **`web`** – REST controllers with security annotations
- **`security`** – Custom ACL permissions and security expressions

### Key Components

```
┌─────────────────────────────────────────────────────────┐
│  REST API Layer (ProjectController, DocumentController) │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│  Security Layer (@PreAuthorize, PermissionEvaluator)    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│  Service Layer (AclPermissionService, DomainServices)   │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│  ACL Infrastructure (MutableAclService, Cache)          │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│  Database (H2) - ACL Tables + Domain Tables             │
└─────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.8+** (or use included Maven wrapper)
- **Git** (for cloning the repository)

### Installation & Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd spring-boot-acl-demo
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application starts on `http://localhost:8080`

4. **Verify ACL setup**
   ```bash
   curl -u admin:admin123 http://localhost:8080/api/acl/status
   ```

   You should see:
   ```json
   {
     "aclSids": 10,
     "aclClasses": 3,
     "aclObjectIdentities": 14,
     "aclEntries": 70,
     "status": "ACL infrastructure is operational"
   }
   ```

### Development Endpoints

| Endpoint | Purpose | Credentials |
|----------|---------|-------------|
| http://localhost:8080/actuator/health | Application health check | None |
| http://localhost:8080/h2-console | H2 database console | JDBC: `jdbc:h2:mem:acldb`<br>User: `sa`<br>Pass: (empty) |
| http://localhost:8080/api/acl/status | ACL infrastructure status | admin:admin123 |

## Spring Security ACL Infrastructure

The application now includes a complete Spring Security ACL (Access Control List) implementation:

### Features

- **ACL Database Schema**: Four core tables (acl_sid, acl_class, acl_object_identity, acl_entry)
- **Stateless REST Security**: HTTP Basic authentication with stateless sessions
- **Method-Level Security**: Support for @PreAuthorize and @PostAuthorize annotations
- **EhCache Integration**: Performance-optimized ACL lookups
- **Sample ACL Data**: Bootstrapped permissions for all Projects and Documents
- **Full CRUD REST APIs**: Complete REST controllers for Project, Document, and Comment with ACL enforcement
- **DTOs & Mappers**: Clean separation between domain entities and API representations
- **Automatic Owner Assignment**: Creators automatically become owners with full permissions
- **ACL Inheritance**: Document → Project, Comment → Document permission cascading
- **Exception Handling**: Comprehensive error responses for access denied and validation failures

### Quick Start

1. Run the application:
   ```bash
   mvn spring-boot:run
   ```

2. Check ACL status:
   ```bash
   curl -u admin:admin123 http://localhost:8080/api/acl/status
   ```

3. Create a project:
   ```bash
   curl -X POST http://localhost:8080/api/projects \
     -u alice:password123 \
     -H "Content-Type: application/json" \
     -d '{"name": "My Project", "description": "Test project", "isPublic": false}'
   ```

4. Test access control:
   ```bash
   # Alice can access her project
   curl -u alice:password123 http://localhost:8080/api/projects/1
   
   # Dave cannot access Alice's private project (403 Forbidden)
   curl -u dave:password123 http://localhost:8080/api/projects/1
   ```

### API Endpoints

The application provides full CRUD REST APIs for:

- **Projects**: `/api/projects` - Create, read, update, delete projects with ACL enforcement
- **Documents**: `/api/documents` - Manage documents within projects with inheritance
- **Comments**: `/api/comments` - Add comments to documents with cascading permissions
- **Permission Management**: `/api/permissions` - Grant, revoke, and query permissions with ACL discovery

#### Permission Management Endpoints

- `POST /api/permissions/grant` - Grant permissions to users, roles, or groups
- `POST /api/permissions/revoke` - Revoke permissions from subjects
- `POST /api/permissions/bulk-update` - Bulk grant or revoke permissions on multiple resources
- `GET /api/permissions/check` - Query effective permissions for current user on a resource
- `GET /api/permissions/accessible` - List all accessible resources of a given type
- `GET /api/permissions/inheritance` - Check permission inheritance chain for a resource
- `GET /api/permissions/available` - List all available permissions
- `GET /api/permissions/custom-demo` - Demonstrate custom permission usage

### Default Users

| Username | Password    | Role    | Groups              | Description |
|----------|-------------|---------|---------------------|-------------|
| admin    | admin123    | ADMIN   | EXECUTIVE, ENGINEERING | Full system access |
| alice    | password123 | MANAGER | ENGINEERING         | Can manage projects |
| bob      | password123 | MEMBER  | ENGINEERING         | Regular team member |
| carol    | password123 | MEMBER  | MARKETING           | Marketing team member |
| dave     | password123 | VIEWER  | SALES               | Read-only access |

---

## 📚 Documentation

### Essential Reading

| Document | Description |
|----------|-------------|
| **[Developer Guide](DEVELOPER_GUIDE.md)** | **START HERE** - Comprehensive guide covering architecture, ACL concepts, and implementation details |
| [API Examples](docs/API_EXAMPLES.md) | Complete REST API examples with curl commands |
| [Permission API](docs/PERMISSION_API.md) | Permission management endpoints reference |
| [Architecture Diagrams](docs/ARCHITECTURE_DIAGRAMS.md) | Visual diagrams of ACL flows and components |
| [OpenAPI Specification](docs/OPENAPI_SPEC.yaml) | Swagger/OpenAPI 3.0 API specification |

### Guides

| Document | Purpose |
|----------|---------|
| [ACL Setup Guide](docs/ACL_SETUP.md) | ACL infrastructure configuration and setup |
| [Domain Model](docs/DOMAIN_MODEL.md) | Entity relationships and ACL design |
| [Testing Guide](docs/TESTING_GUIDE.md) | Comprehensive testing strategies and examples |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common issues and solutions |

### Quick Links

- 🚀 **[5-Minute Quickstart](#quick-start)**
- 🎯 **[ACL Concepts Explained](DEVELOPER_GUIDE.md#acl-concepts)**
- 🔧 **[Extending the System](DEVELOPER_GUIDE.md#extending-the-system)**
- 🧪 **[Running Tests](#running-tests)**
- 🐛 **[Common Issues](docs/TROUBLESHOOTING.md)**

---

## 🧪 Running Tests

The project includes comprehensive test coverage for ACL functionality:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AclServiceIntegrationTests

# Run with coverage report
mvn test jacoco:report
```

### Test Categories

- **Unit Tests**: Service and component isolation tests
- **Integration Tests**: Full Spring context with database
- **REST API Tests**: HTTP endpoint security validation
- **ACL Infrastructure Tests**: ACL setup and configuration verification

See [Testing Guide](docs/TESTING_GUIDE.md) for comprehensive testing documentation.

---

## 🔑 Key Features Explained

### Object-Level Security

Unlike role-based access control (RBAC) which grants permissions based on user roles, ACL provides **per-object permissions**:

```java
// User alice can access Project 1
aclPermissionService.grantToUser(Project.class, 1L, "alice", BasePermission.READ);

// User bob can access Project 2
aclPermissionService.grantToUser(Project.class, 2L, "bob", BasePermission.READ);

// alice cannot access Project 2, bob cannot access Project 1 (object-level security)
```

### Permission Inheritance

Child resources inherit permissions from parent resources:

```
Project 1 (alice: READ, WRITE)
  └─ Document 1 (inherits from Project 1)
      └─ Comment 1 (inherits from Document 1)
          
→ alice has READ and WRITE on all three resources via inheritance
```

### Custom Permissions

Beyond standard permissions (READ, WRITE, DELETE), the system includes:
- **SHARE**: Allow users to share resources with others
- **APPROVE**: For approval workflows on documents

### Audit Trail

All permission changes are logged:

```java
{
  "timestamp": "2024-01-15T10:00:00",
  "operation": "GRANT",
  "resource": "Project:1",
  "subject": "bob",
  "permissions": ["READ", "WRITE"],
  "actor": "admin"
}
```

### Performance Optimization

- **EhCache**: ACL lookups are cached (TTL: 15 minutes)
- **Batch Operations**: Bulk permission grants/revokes
- **Lazy Loading**: ACLs created on-demand
- **Database Indexes**: Optimized ACL table queries

---

## 📖 Example Usage

### Create a Project

```bash
curl -X POST http://localhost:8080/api/projects \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Project",
    "description": "A confidential project",
    "isPublic": false
  }'
```

**Response:**
```json
{
  "id": 1,
  "name": "My Project",
  "ownerUsername": "alice",
  "isPublic": false
}
```

**ACL Behavior**: Alice automatically receives all permissions (READ, WRITE, DELETE, ADMIN, SHARE).

### Grant Permissions

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

**Result**: Bob can now read and modify the project.

### Check Permissions

```bash
curl -X GET "http://localhost:8080/api/permissions/check?resourceType=PROJECT&resourceId=1" \
  -u bob:password123
```

**Response:**
```json
{
  "resourceType": "PROJECT",
  "resourceId": 1,
  "subject": "bob",
  "grantedPermissions": ["READ", "WRITE"],
  "inheritedPermissions": [],
  "hasAccess": true
}
```

### Access Control in Action

```bash
# Bob can now read the project
curl -u bob:password123 http://localhost:8080/api/projects/1
# ✓ 200 OK

# Dave cannot access (no permission)
curl -u dave:password123 http://localhost:8080/api/projects/1
# ✗ 403 Forbidden
```

For more examples, see [API Examples](docs/API_EXAMPLES.md).

---

## 🏗️ Extending the System

### Add Custom Domain Entity

```java
@Entity
public class Task {
    @Id
    @GeneratedValue
    private Long id;
    
    private String title;
    
    @ManyToOne
    private User assignee;
}

// In TaskService
@Transactional
public Task createTask(TaskCreateRequest request) {
    Task task = taskRepository.save(/* ... */);
    
    // Apply ACL ownership
    aclPermissionService.applyOwnership(
        Task.class,
        task.getId(),
        currentUser.getUsername()
    );
    
    return task;
}
```

### Add Custom Permission

```java
public class CustomAclPermission extends BasePermission {
    public static final Permission EXPORT = new CustomAclPermission(128, 'X');
    
    protected CustomAclPermission(int mask, char code) {
        super(mask, code);
    }
}

// Register in AclPermissionRegistry
@Override
public Permission buildFromName(String name) {
    if ("EXPORT".equals(name)) return CustomAclPermission.EXPORT;
    return super.buildFromName(name);
}

// Use in security annotation
@PreAuthorize("hasPermission(#id, 'Document', 'EXPORT')")
public byte[] exportDocument(@PathVariable Long id) {
    // ...
}
```

See [Developer Guide - Extending the System](DEVELOPER_GUIDE.md#extending-the-system) for more details.

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests for new functionality
4. Ensure all tests pass (`mvn test`)
5. Update documentation
6. Commit changes (`git commit -m 'Add amazing feature'`)
7. Push to branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

---

## 📝 License

This project is provided as-is for educational and demonstration purposes.

---

## 🙏 Acknowledgments

- [Spring Security ACL](https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)

---

## 📞 Support

- 📖 **Documentation**: See [Developer Guide](DEVELOPER_GUIDE.md)
- 🐛 **Issues**: Check [Troubleshooting Guide](docs/TROUBLESHOOTING.md)
- 💬 **Questions**: Open an issue in the repository

---

**Version:** 1.0.0  
**Last Updated:** January 2024
