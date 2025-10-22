# ACL Test Suite

## Quick Start

### Run All Tests
```bash
mvn test
```

### Run Specific Test Categories
```bash
# Unit Tests
mvn test -Dtest="*ServiceUnitTests"

# Integration Tests  
mvn test -Dtest="*IntegrationTests"

# REST Endpoint Tests
mvn test -Dtest="SecuredRestEndpointIntegrationTests"

# Negative Path Tests
mvn test -Dtest="AclNegativePathTests"
```

## Test Structure

```
src/test/
├── java/com/example/acl/
│   ├── service/
│   │   ├── AclPermissionServiceUnitTests.java       # Core ACL service unit tests
│   │   ├── AclServiceIntegrationTests.java          # Service layer integration tests
│   │   ├── AclCachingBehaviorTests.java             # Caching behavior tests
│   │   ├── AclGroupAndInheritanceTests.java         # Group and inheritance tests
│   │   └── AclNegativePathTests.java                # Negative path and edge cases
│   ├── web/
│   │   └── SecuredRestEndpointIntegrationTests.java # REST endpoint security tests
│   ├── AclDemoApplicationTests.java                 # Application context tests
│   ├── AclInfrastructureTests.java                  # ACL infrastructure tests
│   └── PermissionManagementControllerTests.java     # Permission management API tests
└── resources/
    └── application-test.properties                  # Test configuration
```

## Test Coverage (120 Total Tests)

### Unit Tests (16 tests)
- ✅ Grant/revoke permissions (user, group, role, authority)
- ✅ Bulk operations
- ✅ Custom permissions (SHARE, APPROVE)
- ✅ Inheritance setup
- ✅ Cache eviction
- ✅ Permission checking

### Integration Tests (49 tests)
- ✅ Service layer interactions
- ✅ Database operations
- ✅ Caching behaviors
- ✅ Group handling
- ✅ Inheritance verification
- ✅ Negative paths and edge cases

### REST Endpoint Tests (24 tests)
- ✅ Owner access (positive path)
- ✅ Shared read access
- ✅ Forbidden operations (negative path)
- ✅ Admin overrides
- ✅ Permission inheritance
- ✅ Validation

### Existing Tests (31 tests)
- ✅ Permission management API
- ✅ ACL infrastructure
- ✅ Application context

## Key Features

### Security Context
Tests use `@WithMockUser` for authentication:
```java
@Test
@WithMockUser(username = "alice", roles = {"MANAGER"})
void testOwnerAccess() {
    // Test with authenticated user context
}
```

### Database Initialization
- H2 in-memory database
- Automatic schema initialization
- Test data from main application
- Transactional rollback after each test

### MockMvc for REST Tests
```java
mockMvc.perform(get("/api/projects/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value(1));
```

## Test Categories

| Category | Focus Area | Test Classes |
|----------|-----------|--------------|
| **Unit** | Core ACL service methods | AclPermissionServiceUnitTests |
| **Integration** | Service interactions | AclServiceIntegrationTests<br/>AclGroupAndInheritanceTests |
| **Caching** | Cache behavior | AclCachingBehaviorTests |
| **Negative** | Error handling & edge cases | AclNegativePathTests |
| **REST** | Secured endpoints | SecuredRestEndpointIntegrationTests |
| **Infrastructure** | ACL setup & database | AclInfrastructureTests |

## Running in CI/CD

Tests are designed for continuous integration:
- **Isolated**: Each test is independent
- **Repeatable**: Deterministic results
- **Fast**: In-memory database (~30 seconds total)
- **Clean**: Transactional rollback

## Troubleshooting

### Tests Not Running
```bash
# Clean and rebuild
mvn clean test

# Run with debug output
mvn test -X
```

### Database Issues
```bash
# Verify schema.sql exists
ls src/main/resources/schema.sql

# Check test properties
cat src/test/resources/application-test.properties
```

### Security Context Issues
Ensure `@WithMockUser` is present or set up in `@BeforeEach`:
```java
@BeforeEach
void setUp() {
    Authentication auth = new UsernamePasswordAuthenticationToken(
        "testuser", "password", 
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

## Documentation

See [TEST_SUITE_DOCUMENTATION.md](../../TEST_SUITE_DOCUMENTATION.md) for comprehensive documentation including:
- Detailed test descriptions
- Coverage summary
- Testing patterns
- Configuration details
- CI/CD guidelines
