# API Examples - ACL-Secured CRUD Operations

This document provides sample API calls demonstrating object-level security, shared permissions, and role-based restrictions.

## Authentication

All endpoints (except public ones) require HTTP Basic Authentication:
- Format: `username:password`
- Example: `alice:password123`

## Default Users

| Username | Password    | Role    |
|----------|-------------|---------|
| admin    | admin123    | ADMIN   |
| alice    | password123 | MANAGER |
| bob      | password123 | MEMBER  |
| carol    | password123 | MEMBER  |
| dave     | password123 | VIEWER  |

---

## Project CRUD Operations

### 1. Create Project (Automatic Owner Assignment)

**Request:**
```bash
curl -X POST http://localhost:8080/api/projects \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Project Alpha",
    "description": "A confidential project",
    "isPublic": false
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Project Alpha",
  "description": "A confidential project",
  "ownerUsername": "alice",
  "isPublic": false,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**ACL Behavior:**
- Automatically assigns ownership to `alice`
- Grants alice: READ, WRITE, DELETE, ADMIN, SHARE permissions
- Other users cannot access unless explicitly granted

---

### 2. Read Project (Object-Level Security)

#### Owner Access - Success
```bash
curl -u alice:password123 http://localhost:8080/api/projects/1
```
**Response:** `200 OK` with project data

#### Non-Owner Access - Denied
```bash
curl -u dave:password123 http://localhost:8080/api/projects/1
```
**Response (403 Forbidden):**
```json
{
  "timestamp": "2024-01-15T10:35:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

#### Admin Override - Success
```bash
curl -u admin:admin123 http://localhost:8080/api/projects/1
```
**Response:** `200 OK` - Admins bypass ACL checks

---

### 3. Update Project (Role-Based Restrictions)

#### Owner Can Update
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Project Alpha v2",
    "description": "Updated description",
    "isPublic": false
  }'
```
**Response:** `200 OK`

#### Non-Owner Cannot Update
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -u bob:password123 \
  -H "Content-Type: application/json" \
  -d '{"name": "Hacked Project"}'
```
**Response:** `403 Forbidden`

---

### 4. Delete Project (ACL Permission Check)

#### Only Owner or ADMIN Can Delete
```bash
curl -X DELETE http://localhost:8080/api/projects/1 \
  -u alice:password123
```
**Response:** `204 No Content`

#### Non-Owner Denied
```bash
curl -X DELETE http://localhost:8080/api/projects/1 \
  -u carol:password123
```
**Response:** `403 Forbidden`

---

### 5. List Projects (PostFilter Security)

```bash
curl -u bob:password123 http://localhost:8080/api/projects
```

**Response:** `200 OK` with array of projects
- Returns **only** projects where bob is owner, has ACL permissions, or are public
- Other projects are filtered out automatically

---

## Document CRUD Operations

### 6. Create Document (Inheritance Linking)

```bash
curl -X POST http://localhost:8080/api/documents \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Design Specification",
    "content": "Detailed design document...",
    "projectId": 1,
    "isPublic": false
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "title": "Design Specification",
  "content": "Detailed design document...",
  "projectId": 1,
  "projectName": "Project Alpha",
  "authorUsername": "alice",
  "isPublic": false,
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**ACL Behavior:**
- Owner: `alice` (author)
- Inherits ACL from parent Project (id: 1)
- If alice has READ on Project, she automatically has READ on this Document

---

### 7. Read Document (Shared Permissions)

#### Scenario: Grant READ permission to bob

First, grant permission programmatically (via AclPermissionService):
```java
aclPermissionService.grantToUser(Document.class, 1L, "bob", BasePermission.READ);
```

Then bob can access:
```bash
curl -u bob:password123 http://localhost:8080/api/documents/1
```
**Response:** `200 OK`

Without the grant:
```bash
curl -u carol:password123 http://localhost:8080/api/documents/1
```
**Response:** `403 Forbidden`

---

### 8. Update Document (Write Permission Required)

```bash
curl -X PUT http://localhost:8080/api/documents/1 \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Design Specification v2",
    "content": "Updated content",
    "isPublic": false
  }'
```
**Response:** `200 OK`

---

### 9. Delete Document

```bash
curl -X DELETE http://localhost:8080/api/documents/1 \
  -u alice:password123
```
**Response:** `204 No Content`

---

## Comment CRUD Operations

### 10. Create Comment (Author Assignment + Inheritance)

```bash
curl -X POST http://localhost:8080/api/comments \
  -u bob:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Great design! I have a few suggestions.",
    "documentId": 1
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "content": "Great design! I have a few suggestions.",
  "documentId": 1,
  "documentTitle": "Design Specification",
  "authorUsername": "bob",
  "createdAt": "2024-01-15T12:00:00",
  "updatedAt": "2024-01-15T12:00:00"
}
```

**ACL Behavior:**
- Author: `bob`
- Inherits ACL from parent Document (id: 1)
- Comment permissions cascade from Document → Project

---

### 11. Read Comment (Author or Permission Required)

#### Author Can Read Own Comment
```bash
curl -u bob:password123 http://localhost:8080/api/comments/1
```
**Response:** `200 OK`

#### Document Owner Can Read (via Inheritance)
```bash
curl -u alice:password123 http://localhost:8080/api/comments/1
```
**Response:** `200 OK` (if alice has READ on parent Document)

#### Unrelated User Cannot Read
```bash
curl -u dave:password123 http://localhost:8080/api/comments/1
```
**Response:** `403 Forbidden`

---

### 12. Update Comment (Author or WRITE Permission)

#### Author Can Update
```bash
curl -X PUT http://localhost:8080/api/comments/1 \
  -u bob:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Updated: Great design! Here are my suggestions..."
  }'
```
**Response:** `200 OK`

#### Non-Author Cannot Update
```bash
curl -X PUT http://localhost:8080/api/comments/1 \
  -u carol:password123 \
  -H "Content-Type: application/json" \
  -d '{"content": "Hijacked comment"}'
```
**Response:** `403 Forbidden`

---

### 13. Delete Comment (Author or DELETE Permission)

```bash
curl -X DELETE http://localhost:8080/api/comments/1 \
  -u bob:password123
```
**Response:** `204 No Content`

---

### 14. List Comments by Document

```bash
curl -u alice:password123 http://localhost:8080/api/comments/document/1
```

**Response:** `200 OK` with array of comments for document 1

---

## Advanced Scenarios

### 15. Public Project Access

Create a public project:
```bash
curl -X POST http://localhost:8080/api/projects \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Open Source Project",
    "description": "Public project for everyone",
    "isPublic": true
  }'
```

Any authenticated user can read:
```bash
curl -u dave:password123 http://localhost:8080/api/projects/2
```
**Response:** `200 OK`

---

### 16. Validation Error Example

```bash
curl -X POST http://localhost:8080/api/projects \
  -u alice:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AB",
    "description": ""
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T13:00:00",
  "status": 400,
  "error": "Validation Failed",
  "validationErrors": {
    "name": "Project name must be between 3 and 100 characters"
  }
}
```

---

### 17. Resource Not Found Example

```bash
curl -u alice:password123 http://localhost:8080/api/projects/999
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T13:05:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Project not found with id: 999"
}
```

---

## Security Features Demonstrated

### ✅ Object-Level Security
- Each resource (Project, Document, Comment) has individual ACL entries
- Access controlled per object, not just by resource type

### ✅ Automatic Owner Assignment
- Creator automatically becomes owner
- Owner receives full permissions (READ, WRITE, DELETE, ADMIN, SHARE)

### ✅ ACL Inheritance
- Documents inherit from Projects
- Comments inherit from Documents
- Cascading permission checks

### ✅ Role-Based Access
- ADMIN users bypass most ACL checks
- Different roles (MANAGER, MEMBER, VIEWER) can be granted different permissions

### ✅ Shared Permissions
- Owners can grant specific permissions to other users
- Fine-grained control (READ only, WRITE without DELETE, etc.)

### ✅ Exception Handling
- 403 Forbidden for access denied
- 400 Bad Request for validation errors
- 401 Unauthorized for missing authentication

---

## Testing Workflow

1. **Start Application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Test Owner Access:**
   - Alice creates a project
   - Alice can read/update/delete it
   - Bob cannot access it

3. **Test Admin Override:**
   - Admin can access any resource regardless of ACL

4. **Test Inheritance:**
   - Alice creates project
   - Alice creates document in project
   - Bob creates comment on document
   - Check permission cascade

5. **Test Public Access:**
   - Create public project
   - Verify all authenticated users can read

6. **Test Validation:**
   - Submit invalid data
   - Verify proper error messages

---

## Next Steps

To grant permissions programmatically (for shared access scenarios), use the `AclPermissionService`:

```java
// Grant READ permission to bob for document 1
aclPermissionService.grantToUser(Document.class, 1L, "bob", BasePermission.READ);

// Grant WRITE permission to a group
aclPermissionService.grantToGroup(Document.class, 1L, Group.DEVELOPERS, 
    BasePermission.READ, BasePermission.WRITE);

// Revoke permission
aclPermissionService.revokePermissions(Document.class, 1L, 
    sidResolver.principalSid("bob"), Arrays.asList(BasePermission.READ));
```

For more details, see:
- [ACL Setup Guide](ACL_SETUP.md)
- [Implementation Summary](../IMPLEMENTATION_SUMMARY.md)
