# Comprehensive ACL Test Suite Documentation

## Overview
This document describes the comprehensive ACL-focused test suite that covers grant/revoke operations, inheritance, custom permissions, group handling, and caching behaviors.

## Test Structure

### 1. Unit Tests

#### AclPermissionServiceUnitTests.java
**Location**: `src/test/java/com/example/acl/service/AclPermissionServiceUnitTests.java`

**Coverage**:
- ✅ ACL creation and initialization (`testEnsureAcl`)
- ✅ Ownership application with default permissions (`testApplyOwnership`)
- ✅ Grant permissions to user (`testGrantToUser`)
- ✅ Grant permissions to group (`testGrantToGroup`)
- ✅ Grant permissions to role (`testGrantToRole`)
- ✅ Grant custom permissions (SHARE, APPROVE) (`testGrantCustomPermissions`)
- ✅ Revoke permissions from user (`testRevokePermissions`)
- ✅ Revoke all permissions for a SID (`testRevokeAllForSid`)
- ✅ Bulk grant operations (`testBulkGrant`)
- ✅ Bulk revoke operations (`testBulkRevoke`)
- ✅ Set parent ACL for inheritance (`testSetParent`)
- ✅ Check hasPermission functionality (`testHasPermission`)
- ✅ Cache eviction (`testEvictCache`)
- ✅ No duplicate permissions (`testNoDuplicatePermissions`)
- ✅ Multiple groups handling (`testMultipleGroups`)
- ✅ Empty permission list handling (`testEmptyPermissionList`)

**Total Tests**: 16

---

### 2. Service Integration Tests

#### AclServiceIntegrationTests.java
**Location**: `src/test/java/com/example/acl/service/AclServiceIntegrationTests.java`

**Coverage**:
- ✅ Owner permissions on created project (`testOwnerPermissionsOnProject`)
- ✅ Group permissions verification (`testGroupPermissions`)
- ✅ Shared user permissions (`testSharedUserPermissions`)
- ✅ Comment inherits from document (`testCommentInheritanceFromDocument`)
- ✅ Document inherits from project (`testDocumentInheritanceFromProject`)
- ✅ Custom permission grant and verification (`testCustomPermissionGrant`)
- ✅ Permission revocation and verification (`testPermissionRevocation`)
- ✅ Cache eviction handling (`testCacheEviction`)
- ✅ All ACL entries are granting (`testAllEntriesAreGranting`)
- ✅ Bulk operations on multiple resources (`testBulkOperations`)
- ✅ Audit log integration (`testAuditLogIntegration`)

**Total Tests**: 11

---

#### AclCachingBehaviorTests.java
**Location**: `src/test/java/com/example/acl/service/AclCachingBehaviorTests.java`

**Coverage**:
- ✅ ACL caching after initial read (`testAclCaching`)
- ✅ Cache eviction on update (`testCacheEvictionOnUpdate`)
- ✅ Manual cache eviction (`testManualCacheEviction`)
- ✅ Parent ACL relationship caching (`testParentAclCaching`)
- ✅ Multiple ACL caching independently (`testMultipleAclCaching`)
- ✅ Permission changes after cache refresh (`testPermissionChangesAfterCacheRefresh`)
- ✅ Cache with multiple SIDs (`testCacheWithMultipleSids`)
- ✅ Cache operations during concurrent modifications (`testCacheDuringConcurrentModifications`)

**Total Tests**: 8

---

#### AclGroupAndInheritanceTests.java
**Location**: `src/test/java/com/example/acl/service/AclGroupAndInheritanceTests.java`

**Coverage**:
- ✅ Grant permissions to multiple groups (`testMultipleGroupPermissions`)
- ✅ Distinguish user vs group permissions (`testUserVsGroupPermissions`)
- ✅ Role permissions (`testRolePermissions`)
- ✅ Parent-child inheritance (`testParentChildInheritance`)
- ✅ Document inherits from project (`testDocumentInheritsFromProject`)
- ✅ Comment inherits from document (`testCommentInheritsFromDocument`)
- ✅ Three-level inheritance: Project > Document > Comment (`testThreeLevelInheritance`)
- ✅ Disable inheritance when specified (`testDisableInheritance`)
- ✅ Revoke group permissions without affecting user permissions (`testRevokeGroupPermissionsOnly`)
- ✅ Mixed SID types in ACL (`testMixedSidTypes`)
- ✅ Real group permissions verification (`testRealGroupPermissions`)
- ✅ Change parent ACL (`testChangeParentAcl`)
- ✅ Bulk grant to multiple groups (`testBulkGrantToGroups`)

**Total Tests**: 13

---

#### AclNegativePathTests.java
**Location**: `src/test/java/com/example/acl/service/AclNegativePathTests.java`

**Coverage**:
- ✅ Non-existent ACL in hasPermission (`testNonExistentAclInHasPermission`)
- ✅ NotFoundException for non-existent ACL (`testNonExistentAclDirect`)
- ✅ Null permission list handling (`testNullPermissionList`)
- ✅ Empty permission list handling (`testEmptyPermissionList`)
- ✅ Revoke non-existent permission (`testRevokeNonExistentPermission`)
- ✅ RevokeAll for SID with no permissions (`testRevokeAllForSidWithNoPermissions`)
- ✅ hasPermission with null authentication (`testHasPermissionWithNullAuth`)
- ✅ Bulk operations with empty ID list (`testBulkOperationsWithEmptyList`)
- ✅ Bulk operations with null ID list (`testBulkOperationsWithNullList`)
- ✅ User without permission denied (`testUserWithoutPermissionDenied`)
- ✅ Read-only user cannot write (`testReadOnlyUserCannotWrite`)
- ✅ Multiple revocations of same permission (`testMultipleRevocations`)
- ✅ No duplicate permissions on multiple calls (`testDuplicatePermissionGrants`)
- ✅ Evict cache on non-cached object (`testEvictCacheOnNonCached`)
- ✅ Invalid permission mask handling (`testInvalidPermissionMask`)
- ✅ No inheritance when disabled (`testNoInheritanceWhenDisabled`)
- ✅ Empty varargs permissions (`testEmptyVarargs`)

**Total Tests**: 17

---

### 3. REST Endpoint Integration Tests

#### SecuredRestEndpointIntegrationTests.java
**Location**: `src/test/java/com/example/acl/web/SecuredRestEndpointIntegrationTests.java`

**Coverage**:

**Owner Access Tests**:
- ✅ Owner full access to their project (`testOwnerFullAccess`)
- ✅ Owner can create a new project (`testCreateProject`)
- ✅ Owner can delete their own project (`testOwnerCanDelete`)
- ✅ Document owner access (`testDocumentOwnerAccess`)
- ✅ Document owner update (`testDocumentOwnerUpdate`)
- ✅ Owner can create a document (`testCreateDocument`)

**Shared Access Tests**:
- ✅ Shared user read access (`testSharedUserReadAccess`)
- ✅ Group member access to shared project (`testGroupBasedAccess`)
- ✅ User with granted permission access (`testGrantedPermissionAccess`)

**Forbidden Operations (Negative Path)**:
- ✅ Non-shared user forbidden (`testNonSharedUserForbidden`)
- ✅ Forbidden update without write permission (`testForbiddenUpdate`)
- ✅ Read-only user cannot delete (`testReadOnlyUserCannotDelete`)
- ✅ User without permission cannot access document (`testDocumentAccessDenied`)
- ✅ After revocation, user denied (`testRevokedPermissionDenied`)
- ✅ Unauthenticated request unauthorized (`testUnauthenticatedAccess`)

**Admin Override Tests**:
- ✅ Admin override and access any project (`testAdminOverride`)
- ✅ Admin can delete any project (`testAdminDelete`)
- ✅ Admin sees all projects (`testAdminSeesAllProjects`)

**Permission Inheritance Tests**:
- ✅ Permission inheritance from project to document (`testPermissionInheritance`)

**Filtered Access Tests**:
- ✅ User sees only accessible projects (`testFilteredProjectList`)
- ✅ Limited project access for non-shared user (`testLimitedProjectAccess`)

**Validation Tests**:
- ✅ Validation on create (`testValidationOnCreate`)
- ✅ Non-existent resource handling (`testNonExistentResource`)

**Total Tests**: 24

---

### 4. Existing Tests (Enhanced)

#### PermissionManagementControllerTests.java
**Coverage**: Permission management REST API endpoints
- Grant/revoke permissions
- Bulk updates
- Effective permissions checking
- Accessible resources listing
- Custom permissions
- Negative paths (forbidden operations)

**Total Tests**: 12

#### AclInfrastructureTests.java
**Coverage**: ACL infrastructure and database setup
- ACL tables verification
- SID creation
- Object identities
- Permission registry
- Audit log

**Total Tests**: 10

#### AclDemoApplicationTests.java
**Coverage**: Application context and data initialization
- Context loading
- Data initialization
- Domain relationships
- User creation
- Project ownership

**Total Tests**: 9

---

## Test Configuration

### application-test.properties
**Location**: `src/test/resources/application-test.properties`

**Configuration**:
```properties
# Test-specific H2 database
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

# Reduced logging for cleaner test output
logging.level.root=WARN
logging.level.com.example.acl=INFO

# Same schema initialization as main application
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
```

---

## Running the Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AclPermissionServiceUnitTests
mvn test -Dtest=SecuredRestEndpointIntegrationTests
mvn test -Dtest=AclCachingBehaviorTests
```

### Run Tests by Pattern
```bash
mvn test -Dtest="*ServiceUnitTests"
mvn test -Dtest="*IntegrationTests"
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report
```

---

## Test Coverage Summary

### Total Test Count by Category

| Category | Test Class | Test Count |
|----------|-----------|-----------|
| **Unit Tests** | AclPermissionServiceUnitTests | 16 |
| **Integration Tests** | AclServiceIntegrationTests | 11 |
| | AclCachingBehaviorTests | 8 |
| | AclGroupAndInheritanceTests | 13 |
| | AclNegativePathTests | 17 |
| | SecuredRestEndpointIntegrationTests | 24 |
| **Existing Tests** | PermissionManagementControllerTests | 12 |
| | AclInfrastructureTests | 10 |
| | AclDemoApplicationTests | 9 |
| **TOTAL** | | **120** |

### Coverage Areas

#### ✅ ACL Service Operations
- [x] Grant permissions (user, group, role, authority)
- [x] Revoke permissions (single, bulk, all for SID)
- [x] Bulk operations (grant, revoke)
- [x] Ownership application
- [x] Permission checking (hasPermission)

#### ✅ Inheritance
- [x] Parent-child ACL relationships
- [x] Three-level inheritance (Project > Document > Comment)
- [x] Inheritance enablement/disablement
- [x] Permission propagation through hierarchy

#### ✅ Custom Permissions
- [x] SHARE permission
- [x] APPROVE permission
- [x] Custom permission grant/revoke
- [x] Custom permission verification

#### ✅ Group Handling
- [x] Multiple groups (ENGINEERING, MARKETING, SALES, OPERATIONS, EXECUTIVE)
- [x] Group permission grants
- [x] Group-based access control
- [x] Group vs user permission distinction

#### ✅ Caching Behaviors
- [x] Cache initialization
- [x] Cache eviction on update
- [x] Manual cache eviction
- [x] Parent ACL caching
- [x] Multiple ACL caching
- [x] Concurrent modification handling

#### ✅ REST Endpoint Security
- [x] Owner access (positive path)
- [x] Shared read access (positive path)
- [x] Forbidden operations (negative path)
- [x] Admin overrides (positive path)
- [x] Permission inheritance verification
- [x] Unauthenticated access denial

#### ✅ Negative Paths
- [x] Non-existent resources
- [x] Null/empty parameters
- [x] Invalid permissions
- [x] Access denial scenarios
- [x] Permission revocation effects
- [x] Duplicate operation handling

---

## Test Annotations Used

### Test Execution
- `@SpringBootTest` - Full application context loading
- `@AutoConfigureMockMvc` - Automatic MockMvc configuration
- `@Transactional` - Rollback after each test
- `@ActiveProfiles("test")` - Use test profile configuration

### Security Context
- `@WithMockUser` - Mock authenticated user with roles
- Custom security context setup in `@BeforeEach` methods

### Test Organization
- `@DisplayName` - Descriptive test names
- `@Test` - JUnit 5 test method marker

---

## Database Initialization

Tests leverage the existing data initialization from the main application:
- H2 in-memory database
- Schema initialization via `schema.sql`
- Test data automatically created on context load
- Each test runs in a transaction that rolls back

---

## Key Testing Patterns

### 1. Arrange-Act-Assert
All tests follow the AAA pattern:
```java
// Arrange
aclPermissionService.ensureAcl(Project.class, projectId);

// Act
aclPermissionService.grantToUser(Project.class, projectId, "alice", BasePermission.READ);

// Assert
assertThat(acl.getEntries()).isNotEmpty();
```

### 2. MockMvc for REST Tests
```java
mockMvc.perform(get("/api/projects/" + projectId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value(projectId));
```

### 3. Security Context Setup
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

### 4. Assertions
Using AssertJ for fluent assertions:
```java
assertThat(acl.getEntries())
    .isNotEmpty()
    .anyMatch(ace -> ace.getSid().equals(userSid));
```

---

## Continuous Integration

These tests are designed to run reliably in CI/CD pipelines:
- **Isolated**: Each test is independent
- **Repeatable**: Same results on every run
- **Fast**: In-memory database
- **Deterministic**: No external dependencies
- **Clean**: Transactional rollback prevents pollution

---

## Future Enhancements

Potential areas for additional testing:
- [ ] Performance tests for bulk operations
- [ ] Concurrent access tests with multiple threads
- [ ] ACL migration/upgrade scenarios
- [ ] Integration with external authentication providers
- [ ] ACL export/import functionality
- [ ] Permission visualization tests

---

## Troubleshooting

### Common Issues

**Tests fail with "ACL not found"**
- Ensure `@Transactional` is present
- Check that `ensureAcl` is called before operations

**Security context not available**
- Verify `@WithMockUser` annotation
- Check `SecurityContextHolder` setup in `@BeforeEach`

**Database initialization fails**
- Verify `schema.sql` exists
- Check `spring.sql.init.mode=always` in test properties

**Cache-related test failures**
- Ensure cache is properly configured
- Check that cache eviction is called when needed

---

## Conclusion

This comprehensive test suite provides **120 tests** covering all aspects of the ACL system:
- Unit tests for core service functionality
- Integration tests for service layer interactions
- REST endpoint security verification
- Positive and negative test paths
- Edge cases and error handling

All tests use Spring Boot Test framework with MockMvc/TestRestTemplate, leverage @WithMockUser for security context, and include database initialization for repeatable test execution via Maven.
