# Domain Model and ACL Design

## Overview

This document describes the domain entities, their relationships, and how Access Control Lists (ACL) are implemented for fine-grained authorization.

## Domain Entities

### User
Represents system users with authentication and authorization information.

**Fields:**
- `id` (Long) - Primary key
- `username` (String) - Unique username (3-50 characters)
- `email` (String) - Unique email address
- `password` (String) - Encrypted password
- `role` (Role enum) - User's role (ADMIN, MANAGER, MEMBER, VIEWER)
- `groups` (Set<Group>) - Groups the user belongs to
- `enabled` (boolean) - Account activation status
- `createdAt` (LocalDateTime) - Audit timestamp
- `updatedAt` (LocalDateTime) - Audit timestamp

**Relationships:**
- One-to-Many: Owned projects
- Many-to-Many: Shared projects
- One-to-Many: Authored documents
- One-to-Many: Comments

### Project
Represents a project container for documents with owner and sharing capabilities.

**Fields:**
- `id` (Long) - Primary key
- `name` (String) - Project name (3-100 characters)
- `description` (String) - Project description (max 500 characters)
- `owner` (User) - Project owner (required)
- `sharedWith` (Set<User>) - Users with access
- `sharedWithGroups` (Set<Group>) - Groups with access
- `isPublic` (boolean) - Public access flag
- `createdAt` (LocalDateTime) - Audit timestamp
- `updatedAt` (LocalDateTime) - Audit timestamp

**Relationships:**
- Many-to-One: Owner (User)
- Many-to-Many: Shared users
- One-to-Many: Documents

### Document
Represents a document within a project with author and sharing capabilities.

**Fields:**
- `id` (Long) - Primary key
- `title` (String) - Document title (1-200 characters)
- `content` (String) - Document content (TEXT)
- `project` (Project) - Parent project (required)
- `author` (User) - Document author (required)
- `sharedWith` (Set<User>) - Users with access
- `sharedWithGroups` (Set<Group>) - Groups with access
- `isPublic` (boolean) - Public access flag
- `createdAt` (LocalDateTime) - Audit timestamp
- `updatedAt` (LocalDateTime) - Audit timestamp

**Relationships:**
- Many-to-One: Project (required)
- Many-to-One: Author (User, required)
- Many-to-Many: Shared users
- One-to-Many: Comments

### Comment
Represents a comment on a document. Comments inherit ACL from their parent document.

**Fields:**
- `id` (Long) - Primary key
- `content` (String) - Comment content (1-1000 characters)
- `document` (Document) - Parent document (required)
- `author` (User) - Comment author (required)
- `createdAt` (LocalDateTime) - Audit timestamp
- `updatedAt` (LocalDateTime) - Audit timestamp

**Relationships:**
- Many-to-One: Document (required)
- Many-to-One: Author (User, required)

**ACL Inheritance:**
Comments inherit access control from their parent Document. Users who can access a Document can also view its comments.

## Enums

### Role
User roles with different privilege levels:
- `ADMIN` - Full system access
- `MANAGER` - Can manage projects and documents
- `MEMBER` - Regular user with basic access
- `VIEWER` - Read-only access

### Group
Organizational groups for team-based sharing:
- `ENGINEERING` - Engineering team
- `MARKETING` - Marketing team
- `SALES` - Sales team
- `OPERATIONS` - Operations team
- `EXECUTIVE` - Executive leadership

## ACL Design

### Access Control Mechanisms

1. **Ownership**
   - Every Project has an `owner` (User)
   - Every Document has an `author` (User)
   - Every Comment has an `author` (User)
   - Owners/authors have full control over their entities

2. **User-based Sharing**
   - Projects and Documents can be shared with specific users via `sharedWith` (Many-to-Many)
   - Allows fine-grained access control at the individual user level

3. **Group-based Sharing**
   - Projects and Documents can be shared with entire groups via `sharedWithGroups` (ElementCollection)
   - Any user belonging to a shared group gets access
   - Simplifies managing access for teams

4. **Public Access**
   - Projects and Documents have an `isPublic` flag
   - Public entities are accessible to all authenticated users
   - Useful for documentation, announcements, etc.

5. **ACL Inheritance**
   - Comments inherit access control from their parent Document
   - No separate ACL configuration needed for comments
   - Simplifies permission management

### Access Check Logic

For a user to access an entity, they must satisfy one of:
1. Be the owner/author of the entity
2. Be in the `sharedWith` set
3. Belong to a group in `sharedWithGroups`
4. The entity has `isPublic = true`
5. For comments: have access to the parent document

## Seed Data

The database initializer creates sample data demonstrating various ACL scenarios:

### Users
1. **admin** (ADMIN, EXECUTIVE + ENGINEERING)
2. **alice** (MANAGER, ENGINEERING)
3. **bob** (MEMBER, ENGINEERING)
4. **carol** (MEMBER, MARKETING)
5. **dave** (VIEWER, SALES)

### Projects
1. **Alice's Engineering Project**
   - Owner: alice
   - Shared with: bob (individual)
   - Shared with: ENGINEERING group
   - Private

2. **Bob's Internal Project**
   - Owner: bob
   - Not shared
   - Private (demonstrates owner-only access)

3. **Public Documentation Project**
   - Owner: admin
   - Public access
   - Demonstrates public content

4. **Marketing Campaign Project**
   - Owner: carol
   - Shared with: MARKETING group
   - Private

### Documents
1. **API Design Document** (Alice's Engineering Project)
   - Author: alice
   - Shared with: bob (individual)
   - Comments: 2 (bob and alice)

2. **Technical Architecture Overview** (Alice's Engineering Project)
   - Author: alice
   - Shared with: ENGINEERING group
   - Comments: 1 (bob)

3. **Bob's Private Notes** (Bob's Internal Project)
   - Author: bob
   - Private (no sharing)

4. **Getting Started Guide** (Public Documentation Project)
   - Author: admin
   - Public access
   - Comments: 1 (dave)

5. **Q4 Marketing Strategy** (Marketing Campaign Project)
   - Author: carol
   - Shared with: MARKETING group
   - Comments: 1 (carol)

### Comments
- Comments demonstrate ACL inheritance
- Users can only comment on documents they can access
- All comments reference parent documents via `document` field

## Database Schema

Key tables created:
- `users` - User entities
- `user_groups` - User to Group mappings
- `projects` - Project entities
- `project_shared_users` - Project sharing with users
- `project_shared_groups` - Project sharing with groups
- `documents` - Document entities
- `document_shared_users` - Document sharing with users
- `document_shared_groups` - Document sharing with groups
- `comments` - Comment entities

## Auditing

All entities include automatic timestamp management:
- `@CreationTimestamp` - Set when entity is created
- `@UpdateTimestamp` - Updated whenever entity is modified
- JPA Auditing enabled via `@EnableJpaAuditing`

## Repositories

Each entity has a corresponding Spring Data JPA repository:
- `UserRepository` - User queries including group fetching
- `ProjectRepository` - Project queries with access control support
- `DocumentRepository` - Document queries with access control support
- `CommentRepository` - Comment queries with document relationships

Repositories include custom queries for:
- Finding entities accessible by a user
- Fetching entities with their relationships (JOIN FETCH)
- Finding entities by owner/author
