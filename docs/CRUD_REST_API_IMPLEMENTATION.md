# CRUD REST API Implementation with ACL Security

## Overview

This document describes the implementation of REST controllers with full CRUD operations for Document, Project, and Comment entities, secured by Spring Security ACL.

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────┐
│    REST Controllers (web)           │
│  - ProjectController                │
│  - DocumentController               │
│  - CommentController                │
└──────────┬──────────────────────────┘
           │ DTOs
           ▼
┌─────────────────────────────────────┐
│    Service Layer (service)          │
│  - ProjectService                   │
│  - DocumentService                  │
│  - CommentService                   │
│  - AclPermissionService             │
└──────────┬──────────────────────────┘
           │ Entities
           ▼
┌─────────────────────────────────────┐
│    Repository Layer                 │
│  - ProjectRepository                │
│  - DocumentRepository               │
│  - CommentRepository                │
│  - UserRepository                   │
└──────────┬──────────────────────────┘
           │ JPA
           ▼
┌─────────────────────────────────────┐
│    Database (H2)                    │
│  - Domain tables                    │
│  - ACL tables                       │
└─────────────────────────────────────┘
```

## Components

### 1. DTOs (Data Transfer Objects)

#### Request DTOs
- **ProjectCreateRequest**: name, description, isPublic
- **ProjectUpdateRequest**: name, description, isPublic (optional)
- **DocumentCreateRequest**: title, content, projectId, isPublic
- **DocumentUpdateRequest**: title, content, isPublic (optional)
- **CommentCreateRequest**: content, documentId
- **CommentUpdateRequest**: content

#### Response DTOs
- **ProjectResponse**: id, name, description, ownerUsername, isPublic, timestamps
- **DocumentResponse**: id, title, content, projectId, projectName, authorUsername, isPublic, timestamps
- **CommentResponse**: id, content, documentId, documentTitle, authorUsername, timestamps

**Benefits:**
- Clean API contracts
- Prevents circular reference issues in JSON
- Hides sensitive entity data
- Allows denormalization (e.g., include project name in document response)

### 2. Mappers

Simple mapping components that convert entities to response DTOs:
- **ProjectMapper**: toResponse(Project)
- **DocumentMapper**: toResponse(Document)
- **CommentMapper**: toResponse(Comment)

### 3. Service Layer

#### ProjectService

```java
@Service
@RequiredArgsConstructor
public class ProjectService {
    // Dependencies
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AclPermissionService aclPermissionService;
    
    // CRUD operations with ACL enforcement
    createProject(request)      // Assigns owner, applies ACL
    getAllProjects()            // @PostFilter
    getProjectById(id)          // @PostAuthorize
    updateProject(id, request)  // @PreAuthorize
    deleteProject(id)           // @PreAuthorize
}
```

**Key Features:**
- Automatic owner assignment from SecurityContext
- ACL ownership application on create
- Permission checks via annotations and manual validation
- Cache eviction on updates/deletes

#### DocumentService

Similar to ProjectService with additional:
- Parent linking: Document → Project inheritance
- Author assignment (instead of owner)

#### CommentService

Similar to DocumentService with:
- Parent linking: Comment → Document inheritance
- Manual permission checks for update/delete (author or ACL permission)
- Additional endpoint: getCommentsByDocumentId(documentId)

### 4. Controllers

RESTful endpoints following standard conventions:

| Method | Path | Operation | Returns |
|--------|------|-----------|---------|
| POST | `/api/projects` | Create | 201 Created + ProjectResponse |
| GET | `/api/projects` | List all | 200 OK + List<ProjectResponse> |
| GET | `/api/projects/{id}` | Get one | 200 OK + ProjectResponse |
| PUT | `/api/projects/{id}` | Update | 200 OK + ProjectResponse |
| DELETE | `/api/projects/{id}` | Delete | 204 No Content |

Same pattern for `/api/documents` and `/api/comments`.

Additional endpoint for comments:
- GET `/api/comments/document/{documentId}` - Get comments for a document

### 5. Exception Handling

**GlobalExceptionHandler** (@RestControllerAdvice) handles:

1. **AccessDeniedException** → 403 Forbidden
   ```json
   {
     "timestamp": "...",
     "status": 403,
     "error": "Forbidden",
     "message": "You do not have permission to access this resource"
   }
   ```

2. **IllegalArgumentException** → 400 Bad Request
   ```json
   {
     "timestamp": "...",
     "status": 400,
     "error": "Bad Request",
     "message": "Project not found with id: 123"
   }
   ```

3. **MethodArgumentNotValidException** → 400 Bad Request
   ```json
   {
     "timestamp": "...",
     "status": 400,
     "error": "Validation Failed",
     "validationErrors": {
       "name": "Project name must be between 3 and 100 characters"
     }
   }
   ```

## ACL Integration

### Create Operation Flow

```
1. User makes POST request with authentication
   ↓
2. Controller validates DTO (@Valid)
   ↓
3. Service extracts username from SecurityContext
   ↓
4. Service creates entity with user as owner/author
   ↓
5. Service saves entity to database
   ↓
6. Service calls aclPermissionService.applyOwnership()
   - Creates ACL entry
   - Grants owner permissions (READ, WRITE, DELETE, ADMIN, SHARE)
   ↓
7. Service calls aclPermissionService.setParent() (for child entities)
   - Links to parent ACL
   - Enables inheritance
   ↓
8. Controller maps entity to response DTO
   ↓
9. Returns 201 Created with response body
```

### Read Operation Flow

```
1. User makes GET request
   ↓
2. Service fetches entity from database
   ↓
3. @PostAuthorize checks permission
   - Evaluates Spring Security expression
   - Checks: ADMIN role OR ACL READ permission OR ownership
   ↓
4. If authorized: Controller returns 200 OK
   If denied: GlobalExceptionHandler returns 403 Forbidden
```

### Update Operation Flow

```
1. User makes PUT request with DTO
   ↓
2. @PreAuthorize checks permission BEFORE execution
   - Checks: ADMIN role OR ownership OR ACL WRITE permission
   ↓
3. If authorized: Service updates entity
   If denied: throws AccessDeniedException → 403
   ↓
4. Service evicts ACL cache
   ↓
5. Controller returns 200 OK with updated DTO
```

### Delete Operation Flow

```
1. User makes DELETE request
   ↓
2. @PreAuthorize checks permission
   - Checks: ADMIN role OR ownership OR ACL DELETE permission
   ↓
3. If authorized: Service deletes entity
   ↓
4. Service evicts ACL cache
   ↓
5. Controller returns 204 No Content
```

## Security Features

### 1. Object-Level Security

Each entity instance has its own ACL entries:
- User alice can access Project 1
- User bob cannot access Project 1
- User bob can access Project 2

### 2. Automatic Owner Assignment

On entity creation:
- Creator automatically becomes owner
- Owner receives full permissions
- ACL entry created automatically

### 3. ACL Inheritance

Permission cascading:
- Project → Document: Documents inherit project permissions
- Document → Comment: Comments inherit document permissions
- Reduces redundant ACL entries

### 4. Role-Based Access

Multiple layers of authorization:
1. **ADMIN role**: Bypasses ACL checks
2. **Ownership**: Owners have full access
3. **ACL permissions**: Fine-grained per-object permissions

### 5. Shared Permissions

Owners can grant permissions to other users:
```java
// Grant READ to bob for document 1
aclPermissionService.grantToUser(Document.class, 1L, "bob", BasePermission.READ);

// Grant WRITE to DEVELOPERS group
aclPermissionService.grantToGroup(Document.class, 1L, Group.DEVELOPERS, 
    BasePermission.READ, BasePermission.WRITE);
```

## Validation

### Request Validation

Jakarta Validation annotations on DTOs:
- `@NotBlank` - Field cannot be empty
- `@NotNull` - Field cannot be null
- `@Size(min, max)` - Length constraints
- `@Email` - Email format validation

Validation triggers automatically via `@Valid` annotation on controller methods.

### Business Validation

Service layer checks:
- Entity existence (throw IllegalArgumentException if not found)
- User authentication (throw AccessDeniedException if not authenticated)
- Permission checks (throw AccessDeniedException if denied)

## Performance Considerations

### ACL Caching

- EhCache integration for ACL lookups
- Cache eviction on updates/deletes
- Reduces database queries for permission checks

### Lazy Loading

- Entities use LAZY fetch for relationships
- Prevents N+1 query problems
- Load associations only when needed

### @PostFilter Performance

`@PostFilter` evaluates permissions per element:
- For large datasets, consider custom queries with ACL filters
- Current implementation suitable for moderate dataset sizes

## Testing Examples

See [API_EXAMPLES.md](API_EXAMPLES.md) for comprehensive testing scenarios including:
- Owner access vs non-owner access
- Admin override
- Public vs private resources
- Inheritance testing
- Validation error handling
- Shared permissions

## Files Created

### DTOs (7 files)
- ProjectCreateRequest.java
- ProjectUpdateRequest.java
- ProjectResponse.java
- DocumentCreateRequest.java
- DocumentUpdateRequest.java
- DocumentResponse.java
- CommentCreateRequest.java
- CommentUpdateRequest.java
- CommentResponse.java

### Mappers (3 files)
- ProjectMapper.java
- DocumentMapper.java
- CommentMapper.java

### Services (3 files)
- ProjectService.java
- DocumentService.java
- CommentService.java

### Controllers (1 new, 2 updated)
- CommentController.java (new)
- ProjectController.java (updated)
- DocumentController.java (updated)

### Exception Handling (1 file)
- GlobalExceptionHandler.java

### Documentation (2 files)
- API_EXAMPLES.md
- CRUD_REST_API_IMPLEMENTATION.md (this file)

## Summary

This implementation provides:
✅ Full CRUD REST APIs for Project, Document, Comment
✅ DTOs and mappers for clean API contracts
✅ Service layer with ACL enforcement
✅ Automatic owner assignment
✅ ACL inheritance linking
✅ Comprehensive exception handling
✅ Object-level security
✅ Role-based access control
✅ Shared permissions support
✅ Validation and error messages
✅ Complete documentation and examples
