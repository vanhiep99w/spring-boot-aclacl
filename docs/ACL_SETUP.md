# Spring Security ACL Setup Guide

## Overview

This document describes the Spring Security ACL (Access Control List) infrastructure that has been set up in this application. ACL provides fine-grained, object-level authorization.

## Components

### 1. Database Schema

The ACL schema consists of four main tables:

- **acl_sid**: Stores security identities (users/authorities)
- **acl_class**: Stores domain object class names
- **acl_object_identity**: Stores object identity information for domain objects
- **acl_entry**: Stores individual permission grants

Schema location: `src/main/resources/schema.sql`

### 2. Security Configuration

**File**: `src/main/java/com/example/acl/config/SecurityConfig.java`

Features:
- Stateless session management (REST-friendly)
- HTTP Basic authentication
- Custom UserDetailsService loading users from database
- Method security enabled with `@EnableMethodSecurity`
- H2 console and health endpoint are publicly accessible

### 3. ACL Configuration

**File**: `src/main/java/com/example/acl/config/AclConfig.java`

Beans configured:
- `AclAuthorizationStrategy`: Controls who can modify ACLs (admin role)
- `PermissionGrantingStrategy`: Determines if permissions are granted
- `AclCache`: EhCache-based caching for ACL lookups
- `LookupStrategy`: BasicLookupStrategy for reading ACL data
- `MutableAclService`: JdbcMutableAclService for managing ACL entries
- `MethodSecurityExpressionHandler`: Enables ACL expressions in security annotations

### 4. ACL Initialization

**File**: `src/main/java/com/example/acl/service/AclInitializationService.java`

This service:
- Bootstraps ACL entries for existing domain objects
- Creates ACL permissions for Projects and Documents
- Grants appropriate permissions to owners and shared users
- Runs after data initialization (`@Order(2)`)

### 5. REST Controllers with ACL

Example controllers demonstrating ACL usage:

- `DocumentController`: CRUD operations with ACL checks
- `ProjectController`: CRUD operations with ACL checks
- `AclTestController`: Diagnostic endpoint for ACL status

## ACL Annotations

### @PreAuthorize

Checks permissions before method execution:

```java
@PreAuthorize("hasPermission(#id, 'com.example.acl.domain.Document', 'WRITE')")
public ResponseEntity<Document> updateDocument(@PathVariable Long id, @RequestBody Document doc) {
    // Only users with WRITE permission on the document can execute this
}
```

### @PostAuthorize

Checks permissions after method execution (useful for filtering return values):

```java
@PostAuthorize("hasPermission(returnObject.body, 'READ')")
public ResponseEntity<Document> getDocument(@PathVariable Long id) {
    // Returns 403 if user doesn't have READ permission on the document
}
```

## Default Permissions

The system uses Spring Security's standard permissions:

- `READ` (mask: 1)
- `WRITE` (mask: 2)
- `CREATE` (mask: 4)
- `DELETE` (mask: 8)
- `ADMINISTRATION` (mask: 16)

## Testing the ACL Setup

### 1. Check ACL Status

```bash
curl -u admin:admin123 http://localhost:8080/api/acl/status
```

Expected response:
```json
{
  "aclSids": 5,
  "aclClasses": 2,
  "aclObjectIdentities": 9,
  "aclEntries": 36,
  "status": "ACL infrastructure is operational"
}
```

### 2. Test Document Access

```bash
# Alice trying to read her own document (should succeed)
curl -u alice:password123 http://localhost:8080/api/documents/1

# Dave trying to read Alice's private document (should fail with 403)
curl -u dave:password123 http://localhost:8080/api/documents/1

# Anyone reading a public document (should succeed)
curl -u dave:password123 http://localhost:8080/api/documents/4
```

### 3. Test Update Permissions

```bash
# Alice updating her own document (should succeed)
curl -u alice:password123 -X PUT \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated Title","content":"Updated content"}' \
  http://localhost:8080/api/documents/1

# Bob trying to update Alice's document without permission (should fail)
curl -u bob:password123 -X PUT \
  -H "Content-Type: application/json" \
  -d '{"title":"Hacked Title","content":"Hacked content"}' \
  http://localhost:8080/api/documents/1
```

## User Credentials

Default users created during initialization:

| Username | Password    | Role    | Groups              |
|----------|-------------|---------|---------------------|
| admin    | admin123    | ADMIN   | EXECUTIVE, ENGINEERING |
| alice    | password123 | MANAGER | ENGINEERING         |
| bob      | password123 | MEMBER  | ENGINEERING         |
| carol    | password123 | MEMBER  | MARKETING           |
| dave     | password123 | VIEWER  | SALES               |

## Architecture Notes

### Stateless REST Security

The application uses stateless session management, which means:
- No server-side sessions
- Each request must include authentication credentials (HTTP Basic)
- Suitable for REST APIs and microservices

### EhCache for ACL

ACL lookups are cached using EhCache to improve performance:
- Reduces database queries
- Automatically invalidated on ACL updates
- Configured in `AclConfig.java`

### H2 Database Compatibility

The schema uses H2-specific syntax:
- `IDENTITY` for auto-increment columns
- `@@IDENTITY` for retrieving generated IDs
- For production with PostgreSQL/MySQL, adjust accordingly

## Extending the ACL System

### Adding Custom Permissions

```java
public class CustomPermission extends BasePermission {
    public static final Permission APPROVE = new CustomPermission(32, 'A');
    
    protected CustomPermission(int mask, char code) {
        super(mask, code);
    }
}
```

### Programmatic ACL Management

Use `AclInitializationService` methods:

```java
// Grant permission
aclInitializationService.grantPermission(document, "username", BasePermission.READ);

// Revoke permission
aclInitializationService.revokePermission(document, "username", BasePermission.WRITE);
```

## Troubleshooting

### ACL Tables Not Created

Check:
1. `spring.sql.init.mode=always` in application.properties
2. `spring.jpa.defer-datasource-initialization=true` to ensure schema runs after Hibernate
3. H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:acldb`)

### Permission Denied Unexpectedly

Debug steps:
1. Check ACL entries: `SELECT * FROM acl_entry`
2. Verify SID exists: `SELECT * FROM acl_sid WHERE sid = 'username'`
3. Enable ACL logging: `logging.level.org.springframework.security.acls=DEBUG`

### Cache Issues

Clear cache or restart application if ACL changes don't take effect.

## References

- [Spring Security ACL Documentation](https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html)
- [Domain Object Security](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html#el-access-web-beans)
