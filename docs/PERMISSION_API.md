# Permission Management and Discovery API

This document describes the REST API endpoints for managing and discovering permissions in the ACL system.

## Base URL

All endpoints are available under `/api/permissions`

## Authentication

All endpoints require authentication. Most management endpoints require `ADMIN` or `MANAGER` roles.

## Available Permissions

The system supports the following permissions:
- `READ` - View the resource
- `WRITE` - Modify the resource
- `CREATE` - Create child resources
- `DELETE` - Delete the resource
- `ADMINISTRATION` - Full control including permission management
- `SHARE` - Custom permission to share resources with others
- `APPROVE` - Custom permission for approval workflows

## Endpoints

### 1. Grant Permissions

Grant permissions to a user, role, or group on a resource.

**Endpoint:** `POST /api/permissions/grant`

**Authorization:** `ADMIN` or `MANAGER` role required

**Request Body:**
```json
{
  "resourceType": "PROJECT",
  "resourceId": 1,
  "subjectType": "USER",
  "subjectIdentifier": "bob",
  "permissions": ["READ", "WRITE"]
}
```

**Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`
- `resourceId` (required): ID of the resource
- `subjectType` (required): `USER`, `ROLE`, or `GROUP`
- `subjectIdentifier` (required): Username, role name (ADMIN, MANAGER, MEMBER, VIEWER), or group name (ENGINEERING, MARKETING, SALES, OPERATIONS, EXECUTIVE)
- `permissions` (required): Array of permission names

**Response:**
```json
{
  "success": true,
  "message": "Permissions granted successfully",
  "resourceType": "PROJECT",
  "resourceId": 1,
  "subject": "USER:bob"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/permissions/grant \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceId": 1,
    "subjectType": "USER",
    "subjectIdentifier": "bob",
    "permissions": ["READ", "WRITE", "SHARE"]
  }'
```

---

### 2. Revoke Permissions

Revoke specific permissions from a subject on a resource.

**Endpoint:** `POST /api/permissions/revoke`

**Authorization:** `ADMIN` or `MANAGER` role required

**Request Body:**
```json
{
  "resourceType": "PROJECT",
  "resourceId": 1,
  "subjectType": "USER",
  "subjectIdentifier": "bob",
  "permissions": ["WRITE"]
}
```

**Parameters:** Same as grant endpoint

**Response:**
```json
{
  "success": true,
  "message": "Permissions revoked successfully",
  "resourceType": "PROJECT",
  "resourceId": 1,
  "subject": "USER:bob"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/permissions/revoke \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceId": 1,
    "subjectType": "USER",
    "subjectIdentifier": "bob",
    "permissions": ["WRITE"]
  }'
```

---

### 3. Bulk Update Permissions

Grant or revoke permissions on multiple resources at once.

**Endpoint:** `POST /api/permissions/bulk-update`

**Authorization:** `ADMIN` or `MANAGER` role required

**Request Body:**
```json
{
  "resourceType": "PROJECT",
  "resourceIds": [1, 2, 3],
  "subjectType": "GROUP",
  "subjectIdentifier": "ENGINEERING",
  "operation": "GRANT",
  "permissions": ["READ", "WRITE"]
}
```

**Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`
- `resourceIds` (required): Array of resource IDs
- `subjectType` (required): `USER`, `ROLE`, or `GROUP`
- `subjectIdentifier` (required): Username, role name, or group name
- `operation` (required): `GRANT` or `REVOKE`
- `permissions` (required): Array of permission names

**Response:**
```json
{
  "success": true,
  "message": "Bulk operation completed successfully",
  "operation": "GRANT",
  "resourcesAffected": 3,
  "resourceType": "PROJECT"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/permissions/bulk-update \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "DOCUMENT",
    "resourceIds": [1, 2, 3],
    "subjectType": "ROLE",
    "subjectIdentifier": "VIEWER",
    "operation": "GRANT",
    "permissions": ["READ"]
  }'
```

---

### 4. Check Effective Permissions

Query the effective permissions for the current user on a specific resource, including inherited permissions.

**Endpoint:** `GET /api/permissions/check`

**Authorization:** Authenticated user

**Query Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`
- `resourceId` (required): ID of the resource

**Response:**
```json
{
  "resourceType": "DOCUMENT",
  "resourceId": 1,
  "subject": "bob",
  "grantedPermissions": ["READ", "WRITE"],
  "inheritedPermissions": ["SHARE"],
  "parentResource": "com.example.acl.domain.Project:1",
  "hasAccess": true
}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/permissions/check?resourceType=DOCUMENT&resourceId=1" \
  -u bob:password123
```

---

### 5. List Accessible Resources

Discover all resources of a given type that the current user can access.

**Endpoint:** `GET /api/permissions/accessible`

**Authorization:** Authenticated user

**Query Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`

**Response:**
```json
{
  "subject": "bob",
  "resourceType": "PROJECT",
  "totalCount": 2,
  "resources": [
    {
      "resourceId": 1,
      "resourceName": "Project Alpha",
      "permissions": ["READ", "WRITE", "ADMINISTRATION"],
      "accessSource": "ACL"
    },
    {
      "resourceId": 2,
      "resourceName": "Project Beta",
      "permissions": ["READ"],
      "accessSource": "ACL"
    }
  ]
}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/permissions/accessible?resourceType=PROJECT" \
  -u bob:password123
```

---

### 6. Check Permission Inheritance

Examine the permission inheritance chain for a specific resource, showing both direct and inherited permissions.

**Endpoint:** `GET /api/permissions/inheritance`

**Authorization:** Authenticated user

**Query Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`
- `resourceId` (required): ID of the resource

**Response:**
```json
{
  "resourceType": "DOCUMENT",
  "resourceId": 1,
  "resourceName": "Important Document",
  "hasParent": true,
  "entriesInheriting": true,
  "directPermissions": ["READ", "WRITE", "DELETE", "ADMINISTRATION", "SHARE"],
  "inheritedPermissions": ["READ", "WRITE"],
  "parent": {
    "resourceType": "com.example.acl.domain.Project",
    "resourceId": 1,
    "resourceName": "Project Alpha",
    "permissions": ["READ", "WRITE", "ADMINISTRATION"]
  }
}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/permissions/inheritance?resourceType=DOCUMENT&resourceId=1" \
  -u bob:password123
```

---

### 7. List Available Permissions

Get a list of all available permissions in the system with descriptions.

**Endpoint:** `GET /api/permissions/available`

**Authorization:** Authenticated user

**Response:**
```json
{
  "permissions": [
    "ADMINISTRATION",
    "APPROVE",
    "CREATE",
    "DELETE",
    "READ",
    "SHARE",
    "WRITE"
  ],
  "description": {
    "READ": "View the resource",
    "WRITE": "Modify the resource",
    "CREATE": "Create child resources",
    "DELETE": "Delete the resource",
    "ADMINISTRATION": "Full control including permission management",
    "SHARE": "Share the resource with others",
    "APPROVE": "Approve changes to the resource"
  }
}
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/permissions/available \
  -u alice:password123
```

---

### 8. Custom Permission Demonstration

Demonstrates the usage of custom permissions (SHARE and APPROVE).

**Endpoint:** `GET /api/permissions/custom-demo`

**Authorization:** `ADMIN` role required

**Query Parameters:**
- `resourceType` (required): `PROJECT`, `DOCUMENT`, or `COMMENT`
- `resourceId` (required): ID of the resource

**Response:**
```json
{
  "resourceType": "DOCUMENT",
  "resourceId": 1,
  "customPermissions": ["SHARE", "APPROVE"],
  "description": {
    "SHARE": "Custom permission allowing users to share this resource with others",
    "APPROVE": "Custom permission for approval workflows"
  },
  "usage": {
    "SHARE": "Grant SHARE permission to allow users to add collaborators",
    "APPROVE": "Grant APPROVE permission for approval workflows on documents"
  },
  "example": "Use POST /api/permissions/grant with permission name 'SHARE' or 'APPROVE'"
}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/permissions/custom-demo?resourceType=DOCUMENT&resourceId=1" \
  -u admin:admin123
```

---

## Use Cases

### Use Case 1: Grant READ permission to a group on multiple projects

```bash
curl -X POST http://localhost:8080/api/permissions/bulk-update \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceIds": [1, 2, 3, 4],
    "subjectType": "GROUP",
    "subjectIdentifier": "MARKETING",
    "operation": "GRANT",
    "permissions": ["READ"]
  }'
```

### Use Case 2: Check what documents a user can access

```bash
curl -X GET "http://localhost:8080/api/permissions/accessible?resourceType=DOCUMENT" \
  -u bob:password123
```

### Use Case 3: Grant custom SHARE permission

```bash
curl -X POST http://localhost:8080/api/permissions/grant \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceId": 1,
    "subjectType": "USER",
    "subjectIdentifier": "carol",
    "permissions": ["SHARE"]
  }'
```

### Use Case 4: Check permission inheritance for a comment

```bash
curl -X GET "http://localhost:8080/api/permissions/inheritance?resourceType=COMMENT&resourceId=1" \
  -u alice:password123
```

### Use Case 5: Revoke WRITE permission from a role

```bash
curl -X POST http://localhost:8080/api/permissions/revoke \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "PROJECT",
    "resourceId": 1,
    "subjectType": "ROLE",
    "subjectIdentifier": "MEMBER",
    "permissions": ["WRITE"]
  }'
```

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "success": false,
  "message": "Error granting permissions: Unknown resource type: INVALID"
}
```

### 401 Unauthorized
Returned when authentication credentials are missing or invalid.

### 403 Forbidden
Returned when the authenticated user doesn't have the required role to access the endpoint.

## Notes

1. **Permission Inheritance**: Documents inherit from Projects, and Comments inherit from Documents when `entriesInheriting` is true.

2. **Subject Types**:
   - `USER`: Individual users (e.g., "alice", "bob")
   - `ROLE`: System roles (ADMIN, MANAGER, MEMBER, VIEWER)
   - `GROUP`: Organizational groups (ENGINEERING, MARKETING, SALES, OPERATIONS, EXECUTIVE)

3. **Resource Types**:
   - `PROJECT`: Top-level projects
   - `DOCUMENT`: Documents within projects
   - `COMMENT`: Comments on documents

4. **Custom Permissions**: The system includes two custom permissions beyond Spring Security's defaults:
   - `SHARE`: For sharing resources
   - `APPROVE`: For approval workflows
