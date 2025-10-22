# Spring Boot ACL Demo

## Overview

This project is a scaffold for exploring Access Control Lists (ACL) in a Spring Boot 3.x application. It will demonstrate how to apply fine-grained authorization rules to domain objects using Spring Security.

## Tech Stack

- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 in-memory database
- Bean Validation (Jakarta Validation)
- Lombok
- Spring Boot Actuator

## Modules

The application is structured using the `com.example.acl` base package with the following sub-packages:

- `config` – application-wide configuration classes
- `domain` – domain entities and value objects
- `repository` – data access components
- `service` – business logic services
- `web` – REST controllers and web entry points
- `security` – security and ACL components

## Running the Application

Ensure Java 17+ and Maven 3.8+ are available locally, then execute:

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### Helpful Endpoints

- Actuator health: `http://localhost:8080/actuator/health`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:acldb`, username: `sa`, password: ` `)

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

### Documentation

- **Permission API**: See [docs/PERMISSION_API.md](docs/PERMISSION_API.md) for detailed permission management documentation
- **API Examples**: See [docs/API_EXAMPLES.md](docs/API_EXAMPLES.md) for detailed usage examples
- **Setup Guide**: See [docs/ACL_SETUP.md](docs/ACL_SETUP.md) for comprehensive documentation
- **Implementation Summary**: See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for technical details

### Default Users

| Username | Password    | Role    |
|----------|-------------|---------|
| admin    | admin123    | ADMIN   |
| alice    | password123 | MANAGER |
| bob      | password123 | MEMBER  |
| carol    | password123 | MEMBER  |
| dave     | password123 | VIEWER  |
