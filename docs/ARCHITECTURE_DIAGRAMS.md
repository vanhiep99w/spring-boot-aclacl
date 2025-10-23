# ACL Demo - Architecture Diagrams

This document contains visual diagrams explaining the ACL system architecture, flows, and component interactions.

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [ACL Permission Check Flow](#acl-permission-check-flow)
3. [Permission Inheritance Hierarchy](#permission-inheritance-hierarchy)
4. [ACL Database Schema](#acl-database-schema)
5. [Caching Architecture](#caching-architecture)
6. [Audit Trail Flow](#audit-trail-flow)
7. [Component Interaction](#component-interaction)
8. [Request Processing Flow](#request-processing-flow)

---

## System Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        Client[REST Client / cURL]
    end
    
    subgraph "API Layer"
        PC[ProjectController]
        DC[DocumentController]
        CC[CommentController]
        PMC[PermissionManagementController]
    end
    
    subgraph "Security Layer"
        SA[Security Annotations<br/>@PreAuthorize/@PostAuthorize]
        MSH[MethodSecurityExpressionHandler]
        PE[Permission Evaluator]
    end
    
    subgraph "Service Layer"
        PS[ProjectService]
        DS[DocumentService]
        CS[CommentService]
        APS[AclPermissionService]
        PDS[PermissionDiscoveryService]
        AAS[AclAuditService]
    end
    
    subgraph "ACL Infrastructure"
        MACL[MutableAclService<br/>JDBC-based]
        AC[AclCache<br/>EhCache]
        LS[LookupStrategy]
        PR[PermissionRegistry]
    end
    
    subgraph "Data Layer"
        DB[(H2 Database)]
        ACL_T[(ACL Tables:<br/>acl_sid, acl_class<br/>acl_object_identity<br/>acl_entry)]
        DOM_T[(Domain Tables:<br/>users, projects<br/>documents, comments)]
    end
    
    Client -->|HTTP Basic Auth| PC
    Client -->|HTTP Basic Auth| DC
    Client -->|HTTP Basic Auth| CC
    Client -->|HTTP Basic Auth| PMC
    
    PC --> SA
    DC --> SA
    CC --> SA
    PMC --> SA
    
    SA --> MSH
    MSH --> PE
    PE --> AC
    AC -->|Cache Miss| MACL
    
    PC --> PS
    DC --> DS
    CC --> CS
    PMC --> APS
    PMC --> PDS
    
    PS --> APS
    DS --> APS
    CS --> APS
    APS --> MACL
    APS --> AC
    APS --> AAS
    PDS --> MACL
    
    MACL --> LS
    LS --> ACL_T
    PS --> DOM_T
    DS --> DOM_T
    CS --> DOM_T
    
    ACL_T --> DB
    DOM_T --> DB
    
    style Client fill:#e1f5ff
    style SA fill:#fff3cd
    style AC fill:#d4edda
    style DB fill:#f8d7da
```

---

## ACL Permission Check Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Security
    participant PermEval as Permission Evaluator
    participant Cache as ACL Cache
    participant Service as ACL Service
    participant DB as Database
    
    Client->>Controller: GET /api/projects/1<br/>(alice:password123)
    Controller->>Security: @PreAuthorize triggered<br/>hasPermission(#id, 'Project', 'READ')
    Security->>Security: Extract authentication<br/>Retrieve SIDs for alice
    Note over Security: SIDs: alice (principal)<br/>ROLE_MANAGER<br/>GROUP_ENGINEERING
    
    Security->>PermEval: hasPermission(auth, Project:1, READ)
    PermEval->>Cache: Check cache for Project:1 ACL
    
    alt Cache Hit
        Cache-->>PermEval: Return cached ACL
    else Cache Miss
        Cache->>Service: Load ACL from database
        Service->>DB: Query acl_object_identity<br/>acl_entry for Project:1
        DB-->>Service: Return ACL data
        Service-->>Cache: Store in cache
        Cache-->>PermEval: Return ACL
    end
    
    PermEval->>PermEval: Check if any SID has READ permission
    Note over PermEval: Check direct entries<br/>Check inherited entries
    
    alt Permission Granted
        PermEval-->>Security: true
        Security-->>Controller: Access granted
        Controller->>Controller: Execute method
        Controller-->>Client: 200 OK + Project data
    else Permission Denied
        PermEval-->>Security: false
        Security-->>Controller: AccessDeniedException
        Controller-->>Client: 403 Forbidden
    end
```

---

## Permission Inheritance Hierarchy

```mermaid
graph TD
    subgraph "Project Level"
        P1[Project: Engineering Project<br/>ID: 1<br/>Owner: alice]
        P1_ACL[ACL Entries:<br/>✓ alice: ALL<br/>✓ bob: READ<br/>✓ GROUP_ENGINEERING: READ, WRITE]
        P1 --> P1_ACL
    end
    
    subgraph "Document Level"
        D1[Document: API Design<br/>ID: 1<br/>Author: alice<br/>Parent: Project:1<br/>entriesInheriting: true]
        D1_ACL[Direct ACL:<br/>✓ alice: ALL<br/><br/>Inherited from Project:1:<br/>✓ bob: READ<br/>✓ GROUP_ENGINEERING: READ, WRITE]
        D1 --> D1_ACL
    end
    
    subgraph "Comment Level"
        C1[Comment: Great design!<br/>ID: 1<br/>Author: bob<br/>Parent: Document:1<br/>entriesInheriting: true]
        C1_ACL[Direct ACL:<br/>✓ bob: ALL<br/><br/>Inherited from Document:1:<br/>✓ alice: ALL<br/>✓ GROUP_ENGINEERING: READ, WRITE<br/><br/>Inherited from Project:1:<br/>✓ alice: ALL<br/>✓ bob: READ<br/>✓ GROUP_ENGINEERING: READ, WRITE]
        C1 --> C1_ACL
    end
    
    P1 -.->|inherits| D1
    D1 -.->|inherits| C1
    
    style P1 fill:#e3f2fd
    style D1 fill:#fff3e0
    style C1 fill:#f3e5f5
    style P1_ACL fill:#c8e6c9
    style D1_ACL fill:#c8e6c9
    style C1_ACL fill:#c8e6c9
```

### Inheritance Rules

1. **entriesInheriting = true**: Child inherits parent's ACL entries
2. **Multiple Levels**: Inheritance cascades through multiple levels
3. **Permission Union**: Effective permissions = Direct + Inherited
4. **Parent Must Exist**: Parent ACL must be created before setting inheritance
5. **Order of Checking**: Direct entries checked first, then inherited entries

---

## ACL Database Schema

```mermaid
erDiagram
    ACL_SID {
        bigint id PK
        boolean principal
        varchar sid UK
    }
    
    ACL_CLASS {
        bigint id PK
        varchar class UK
        varchar class_id_type
    }
    
    ACL_OBJECT_IDENTITY {
        bigint id PK
        bigint object_id_class FK
        varchar object_id_identity UK
        bigint parent_object FK
        bigint owner_sid FK
        boolean entries_inheriting
    }
    
    ACL_ENTRY {
        bigint id PK
        bigint acl_object_identity FK
        int ace_order UK
        bigint sid FK
        int mask
        boolean granting
        boolean audit_success
        boolean audit_failure
    }
    
    ACL_SID ||--o{ ACL_ENTRY : "has"
    ACL_SID ||--o{ ACL_OBJECT_IDENTITY : "owns"
    ACL_CLASS ||--o{ ACL_OBJECT_IDENTITY : "identifies"
    ACL_OBJECT_IDENTITY ||--o{ ACL_ENTRY : "has"
    ACL_OBJECT_IDENTITY ||--o{ ACL_OBJECT_IDENTITY : "parent"
```

### Table Descriptions

#### acl_sid (Security Identity)
Stores subjects (users, roles, groups):
```
| id | principal | sid                 |
|----|-----------|---------------------|
| 1  | true      | alice               |
| 2  | false     | ROLE_ADMIN          |
| 3  | false     | GROUP_ENGINEERING   |
```

#### acl_class (Domain Class)
Stores domain object types:
```
| id | class                           |
|----|---------------------------------|
| 1  | com.example.acl.domain.Project  |
| 2  | com.example.acl.domain.Document |
| 3  | com.example.acl.domain.Comment  |
```

#### acl_object_identity (Object Instance)
Stores individual resource instances:
```
| id | object_id_class | object_id_identity | parent | owner_sid | entries_inheriting |
|----|-----------------|--------------------| -------|-----------|-------------------|
| 1  | 1               | 1                  | NULL   | 1         | true              |
| 2  | 2               | 1                  | 1      | 1         | true              |
| 3  | 3               | 1                  | 2      | 2         | true              |
```

#### acl_entry (Permission Grant)
Stores permission entries:
```
| id | acl_object_identity | ace_order | sid | mask | granting |
|----|---------------------|-----------|-----|------|----------|
| 1  | 1                   | 0         | 1   | 31   | true     |
| 2  | 1                   | 1         | 2   | 1    | true     |
| 3  | 2                   | 0         | 1   | 31   | true     |
```

**Permission Masks:**
- READ = 1
- WRITE = 2
- CREATE = 4
- DELETE = 8
- ADMINISTRATION = 16
- SHARE = 32
- APPROVE = 64
- ALL (R+W+C+D+A) = 31

---

## Caching Architecture

```mermaid
graph TB
    subgraph "Application Layer"
        App[Application Code]
    end
    
    subgraph "ACL Service Layer"
        APS[AclPermissionService]
        PE[PermissionEvaluator]
    end
    
    subgraph "Cache Layer"
        AC[EhCache<br/>ACL Cache]
        CM[Cache Manager]
        CE[Cache Eviction<br/>Strategy]
    end
    
    subgraph "Database Layer"
        MACL[MutableAclService<br/>JDBC]
        DB[(Database)]
    end
    
    App -->|Check Permission| PE
    App -->|Grant/Revoke| APS
    
    PE -->|1. Check Cache| AC
    AC -->|Cache Hit| PE
    AC -->|2. Cache Miss| MACL
    MACL -->|3. Query| DB
    DB -->|4. Return Data| MACL
    MACL -->|5. Store| AC
    AC -->|6. Return ACL| PE
    
    APS -->|Update ACL| MACL
    MACL -->|Write| DB
    MACL -->|Evict| AC
    
    CM -.->|Manages| AC
    CE -.->|TTL: 900s<br/>Idle: 300s<br/>LRU Policy| AC
    
    style AC fill:#d4edda
    style DB fill:#f8d7da
    style CE fill:#fff3cd
```

### Cache Configuration

```java
CacheConfiguration:
  - Name: aclCache
  - Eviction Policy: LRU (Least Recently Used)
  - Time to Live: 900 seconds (15 minutes)
  - Time to Idle: 300 seconds (5 minutes)
  - Max Entries: 2048
  - Eternal: false
```

### Cache Behavior

1. **Read Path**:
   - Check cache first
   - On miss, load from database and cache
   - Return ACL to caller

2. **Write Path**:
   - Update database
   - Evict affected cache entries
   - Next read will reload from database

3. **Automatic Eviction**:
   - TTL expired
   - Idle time exceeded
   - LRU when capacity reached

4. **Manual Eviction**:
   - On ACL updates via `aclService.updateAcl()`
   - Via `AclPermissionService.evictCache()`

---

## Audit Trail Flow

```mermaid
sequenceDiagram
    participant Admin
    participant PMC as PermissionController
    participant APS as AclPermissionService
    participant MACL as MutableAclService
    participant AAS as AclAuditService
    participant Store as AuditLogStore
    participant EventBus as ApplicationEventPublisher
    participant Listener as AclAuditEventListener
    
    Admin->>PMC: POST /api/permissions/grant<br/>{resourceType: "PROJECT", resourceId: 1, ...}
    PMC->>APS: grantToUser(Project, 1, "bob", READ)
    
    APS->>MACL: ensureAcl(Project, 1)
    MACL-->>APS: Return MutableAcl
    
    APS->>APS: Add permission entry
    APS->>MACL: updateAcl(acl)
    MACL->>MACL: Write to database
    MACL-->>APS: Success
    
    APS->>AAS: publishChange(GRANT, Project, 1, bob, [READ], "admin")
    
    AAS->>AAS: Create AclAuditLogEntry
    Note over AAS: {<br/>  operation: GRANT<br/>  domainClass: Project<br/>  identifier: 1<br/>  sid: bob<br/>  permissions: [READ]<br/>  actor: admin<br/>  timestamp: 2024-01-15T10:00:00<br/>}
    
    AAS->>Store: save(logEntry)
    Store-->>AAS: Saved
    
    AAS->>EventBus: publishEvent(AclPermissionChangeEvent)
    EventBus->>Listener: onPermissionChange(event)
    
    Listener->>Listener: Process event<br/>(e.g., send notification, webhook, etc.)
    
    AAS-->>APS: Audit complete
    APS-->>PMC: Success
    PMC-->>Admin: 200 OK + Response
```

### Audit Log Entry Structure

```json
{
  "id": "uuid",
  "timestamp": "2024-01-15T10:00:00",
  "operation": "GRANT",
  "domainClass": "com.example.acl.domain.Project",
  "identifier": "1",
  "sid": "bob",
  "permissions": ["READ", "WRITE"],
  "actor": "admin",
  "metadata": {
    "ipAddress": "192.168.1.100",
    "userAgent": "curl/7.68.0"
  }
}
```

### Audit Operations

- **GRANT**: Permission granted to a subject
- **REVOKE**: Permission revoked from a subject
- **OWNERSHIP**: Ownership assigned or changed
- **INHERITANCE**: Parent-child relationship established
- **CREATE**: New ACL created

---

## Component Interaction

```mermaid
graph LR
    subgraph "Controllers"
        PC[ProjectController]
        DC[DocumentController]
        PMC[PermissionController]
    end
    
    subgraph "Services"
        PS[ProjectService]
        DS[DocumentService]
        APS[AclPermissionService]
        PDS[PermissionDiscoveryService]
        AAS[AclAuditService]
    end
    
    subgraph "Security Components"
        SR[SidResolver]
        PR[PermissionRegistry]
        MSE[MethodSecurity<br/>ExpressionHandler]
    end
    
    subgraph "ACL Infrastructure"
        MACL[MutableAclService]
        Cache[AclCache]
    end
    
    subgraph "Repositories"
        PR_Repo[ProjectRepository]
        DR[DocumentRepository]
        UR[UserRepository]
    end
    
    PC --> PS
    DC --> DS
    PMC --> APS
    PMC --> PDS
    
    PS --> APS
    PS --> PR_Repo
    DS --> APS
    DS --> DR
    
    APS --> MACL
    APS --> Cache
    APS --> SR
    APS --> AAS
    PDS --> MACL
    
    MSE --> MACL
    MSE --> PR
    
    MACL --> Cache
    
    SR --> UR
    
    style APS fill:#fff3cd
    style MACL fill:#d4edda
    style Cache fill:#d4edda
```

---

## Request Processing Flow

### Create Project with ACL

```mermaid
graph TD
    Start[Client: POST /api/projects] --> Auth{Authenticated?}
    Auth -->|No| Unauth[401 Unauthorized]
    Auth -->|Yes| Validate{Valid Request?}
    
    Validate -->|No| ValErr[400 Bad Request]
    Validate -->|Yes| CreateEntity[Create Project Entity<br/>owner = currentUser]
    
    CreateEntity --> SaveDB[Save to Database<br/>projectRepository.save]
    SaveDB --> CreateACL[Create ACL Entry<br/>aclService.createAcl]
    
    CreateACL --> SetOwner[Set ACL Owner<br/>acl.setOwner]
    SetOwner --> GrantOwner[Grant Owner Permissions<br/>READ, WRITE, DELETE, ADMIN, SHARE]
    
    GrantOwner --> UpdateACL[Update ACL<br/>aclService.updateAcl]
    UpdateACL --> Audit[Audit Log<br/>OWNERSHIP operation]
    
    Audit --> Return[200 Created<br/>Return ProjectResponse]
    Return --> End[End]
    
    style CreateEntity fill:#e3f2fd
    style CreateACL fill:#fff3cd
    style GrantOwner fill:#c8e6c9
    style Audit fill:#f3e5f5
```

### Check Permission with Inheritance

```mermaid
graph TD
    Start[Check Permission<br/>Document:1, READ] --> GetAuth[Get Current Authentication<br/>Extract SIDs]
    
    GetAuth --> CheckCache{ACL in Cache?}
    CheckCache -->|Yes| UseCached[Use Cached ACL]
    CheckCache -->|No| LoadDB[Load from Database]
    LoadDB --> StoreCache[Store in Cache]
    StoreCache --> UseCached
    
    UseCached --> CheckDirect{Direct Permission<br/>Found?}
    CheckDirect -->|Yes| GrantAccess[Grant Access]
    CheckDirect -->|No| HasParent{Has Parent ACL?}
    
    HasParent -->|No| DenyAccess[Deny Access]
    HasParent -->|Yes| Inheriting{entriesInheriting<br/>= true?}
    
    Inheriting -->|No| DenyAccess
    Inheriting -->|Yes| CheckParent[Check Parent ACL<br/>Project:1]
    
    CheckParent --> ParentPerm{Parent Permission<br/>Found?}
    ParentPerm -->|Yes| GrantAccess
    ParentPerm -->|No| CheckGrandparent{Has Grandparent?}
    
    CheckGrandparent -->|Yes| Inheriting
    CheckGrandparent -->|No| DenyAccess
    
    GrantAccess --> Success[200 OK<br/>Return Resource]
    DenyAccess --> Forbidden[403 Forbidden]
    
    style GrantAccess fill:#c8e6c9
    style DenyAccess fill:#ffcdd2
    style CheckCache fill:#fff3cd
```

### Bulk Permission Grant

```mermaid
graph TD
    Start[POST /api/permissions/bulk-update] --> AuthCheck{Has ADMIN/MANAGER<br/>Role?}
    
    AuthCheck -->|No| Forbidden[403 Forbidden]
    AuthCheck -->|Yes| Parse[Parse Request<br/>resourceIds: [1,2,3]<br/>subject: bob<br/>permissions: [READ,WRITE]]
    
    Parse --> ResolveSID[Resolve SID<br/>for subject 'bob']
    ResolveSID --> ResolvePerms[Resolve Permissions<br/>READ, WRITE]
    
    ResolvePerms --> Loop{For Each<br/>Resource ID}
    
    Loop --> EnsureACL[Ensure ACL Exists<br/>for Resource]
    EnsureACL --> AddPerms[Add Permissions<br/>if Missing]
    AddPerms --> UpdateACL[Update ACL]
    UpdateACL --> Audit[Audit Log Entry]
    
    Audit --> Loop
    Loop -->|All Done| Response[200 OK<br/>resourcesAffected: 3]
    
    style AuthCheck fill:#fff3cd
    style Loop fill:#e3f2fd
    style Audit fill:#f3e5f5
```

---

## Permission Discovery Flow

```mermaid
sequenceDiagram
    participant User as User (bob)
    participant API as /api/permissions/accessible
    participant PDS as PermissionDiscoveryService
    participant ACL as AclService
    participant DB as Database
    
    User->>API: GET /accessible?resourceType=PROJECT
    API->>PDS: findAccessibleResources(Project, bob)
    
    PDS->>PDS: Get all SIDs for bob<br/>(bob, ROLE_MEMBER, GROUP_ENGINEERING)
    
    PDS->>DB: Query: Find all acl_object_identity<br/>WHERE acl_entry.sid IN (bob's SIDs)<br/>AND acl_class = Project
    
    DB-->>PDS: Return matching object identities:<br/>[Project:1, Project:3, Project:5]
    
    loop For each Project ID
        PDS->>ACL: readAclById(Project:N)
        ACL-->>PDS: Return ACL
        PDS->>PDS: Calculate effective permissions<br/>(direct + inherited)
    end
    
    PDS->>PDS: Build ResourceAccessInfo:<br/>- resourceId<br/>- resourceName<br/>- permissions<br/>- accessSource
    
    PDS-->>API: Return accessible resources list
    API-->>User: 200 OK + {<br/>  totalCount: 3,<br/>  resources: [...]<br/>}
```

---

## Summary

These diagrams illustrate:

1. **System Architecture**: Overall component layout and dependencies
2. **Permission Check Flow**: How ACL permissions are evaluated at runtime
3. **Inheritance Hierarchy**: How permissions cascade through parent-child relationships
4. **Database Schema**: ACL table structure and relationships
5. **Caching Architecture**: How EhCache optimizes ACL lookups
6. **Audit Trail Flow**: How permission changes are logged and tracked
7. **Component Interaction**: How services, repositories, and ACL infrastructure interact
8. **Request Processing**: End-to-end flows for common operations

For implementation details, see the [Developer Guide](../DEVELOPER_GUIDE.md).
