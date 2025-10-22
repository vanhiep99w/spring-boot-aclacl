# Troubleshooting Guide - Spring Boot ACL Demo

This guide helps diagnose and resolve common issues when working with the ACL demo application.

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Authentication & Authorization Issues](#authentication--authorization-issues)
3. [ACL Infrastructure Issues](#acl-infrastructure-issues)
4. [Permission & Access Issues](#permission--access-issues)
5. [Inheritance Issues](#inheritance-issues)
6. [Cache Issues](#cache-issues)
7. [Performance Issues](#performance-issues)
8. [Database Issues](#database-issues)
9. [Configuration Issues](#configuration-issues)
10. [Development Tips](#development-tips)

---

## Quick Diagnostics

### Check ACL Infrastructure Status

```bash
curl -u admin:admin123 http://localhost:8080/api/acl/status
```

**Expected Response:**
```json
{
  "aclSids": 10,
  "aclClasses": 3,
  "aclObjectIdentities": 14,
  "aclEntries": 70,
  "status": "ACL infrastructure is operational"
}
```

**If Failed:**
- Check application is running
- Verify database is initialized
- Check credentials (admin:admin123)

### Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

### Check Logs

```bash
# View application logs
tail -f logs/application.log

# Or if running with mvn spring-boot:run
# Check console output
```

### Enable Debug Logging

Add to `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.acls=DEBUG
logging.level.com.example.acl=DEBUG
```

---

## Authentication & Authorization Issues

### Issue: 401 Unauthorized

**Symptom:**
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

**Causes & Solutions:**

1. **Missing or Invalid Credentials**
   ```bash
   # Wrong
   curl http://localhost:8080/api/projects
   
   # Correct
   curl -u alice:password123 http://localhost:8080/api/projects
   ```

2. **Wrong Username/Password**
   
   Default users:
   - admin / admin123
   - alice / password123
   - bob / password123
   - carol / password123
   - dave / password123

3. **Check User Exists in Database**
   ```sql
   SELECT * FROM users WHERE username = 'alice';
   ```

### Issue: 403 Forbidden

**Symptom:**
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

**Causes & Solutions:**

1. **User Doesn't Have Required Permission**
   
   Check user's permissions:
   ```bash
   curl -u alice:password123 \
     "http://localhost:8080/api/permissions/check?resourceType=PROJECT&resourceId=1"
   ```
   
   Grant permission if needed:
   ```bash
   curl -X POST http://localhost:8080/api/permissions/grant \
     -u admin:admin123 \
     -H "Content-Type: application/json" \
     -d '{
       "resourceType": "PROJECT",
       "resourceId": 1,
       "subjectType": "USER",
       "subjectIdentifier": "alice",
       "permissions": ["READ"]
     }'
   ```

2. **Wrong Resource ID**
   
   Verify resource exists:
   ```sql
   SELECT * FROM projects WHERE id = 1;
   ```

3. **User Accessing Another User's Private Resource**
   
   This is expected behavior - private resources are only accessible to:
   - Owner
   - Users with explicit permissions
   - Members of groups with permissions
   - Admins

### Issue: CSRF Token Required

**Symptom:**
```
Invalid CSRF token
```

**Solution:**
CSRF is disabled for stateless REST API. If you're seeing this, check:

```java
// In SecurityConfig.java - should be disabled
.csrf(csrf -> csrf.disable())
```

---

## ACL Infrastructure Issues

### Issue: ACL Tables Not Created

**Symptom:**
```
Table 'acl_sid' doesn't exist
```

**Diagnosis:**

1. **Check schema.sql exists**
   ```bash
   ls -la src/main/resources/schema.sql
   ```

2. **Check SQL init configuration**
   ```properties
   # In application.properties
   spring.sql.init.mode=always
   spring.jpa.defer-datasource-initialization=true
   ```

3. **Check for SQL errors in logs**
   ```bash
   grep -i "error" logs/application.log | grep -i "schema"
   ```

**Solutions:**

1. **Enable SQL logging**
   ```properties
   spring.jpa.show-sql=true
   spring.sql.init.mode=always
   ```

2. **Manually verify H2 console**
   - Navigate to: http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:mem:acldb`
   - Username: `sa`
   - Password: (empty)
   - Check if ACL tables exist

3. **Force schema recreation**
   ```properties
   spring.jpa.hibernate.ddl-auto=create-drop
   ```

### Issue: ACL Service Not Found

**Symptom:**
```
No qualifying bean of type 'org.springframework.security.acls.model.MutableAclService'
```

**Solution:**

Check `AclConfig.java` has:
```java
@Configuration
public class AclConfig {
    
    @Bean
    public MutableAclService aclService() {
        // Configuration
    }
}
```

### Issue: Method Security Not Working

**Symptom:**
Security annotations like `@PreAuthorize` are ignored.

**Solution:**

Ensure `@EnableMethodSecurity` is present:
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // Configuration
}
```

---

## Permission & Access Issues

### Issue: Permission Granted But Still Access Denied

**Diagnosis Steps:**

1. **Verify permission was actually granted**
   ```sql
   -- Check ACL entry
   SELECT e.*, s.sid, s.principal, e.mask
   FROM acl_entry e
   JOIN acl_sid s ON e.sid = s.id
   JOIN acl_object_identity oi ON e.acl_object_identity = oi.id
   WHERE oi.object_id_identity = '1'
   AND oi.object_id_class = (
       SELECT id FROM acl_class WHERE class = 'com.example.acl.domain.Project'
   );
   ```

2. **Check permission mask**
   
   Permission masks:
   - READ = 1
   - WRITE = 2
   - CREATE = 4
   - DELETE = 8
   - ADMINISTRATION = 16
   - SHARE = 32
   - APPROVE = 64

3. **Check SID matches**
   ```sql
   SELECT * FROM acl_sid WHERE sid = 'alice';
   ```

**Common Causes:**

1. **Cache Not Evicted**
   
   Solution: Clear cache manually
   ```java
   aclPermissionService.evictCache(Project.class, projectId);
   ```
   
   Or restart application

2. **Permission Granted to Wrong Subject**
   
   Example: Granted to user "bob" but checking as "Bob" (case-sensitive)
   
   Solution: Verify exact username

3. **Wrong Permission Type**
   
   Example: Granted READ but checking WRITE
   
   Solution: Grant correct permission

### Issue: Owner Cannot Access Own Resource

**Symptom:**
User who created a resource gets 403 Forbidden.

**Diagnosis:**

```sql
-- Check ownership
SELECT oi.*, s.sid as owner
FROM acl_object_identity oi
JOIN acl_sid s ON oi.owner_sid = s.id
WHERE oi.object_id_identity = '1';

-- Check owner's permissions
SELECT e.mask
FROM acl_entry e
JOIN acl_object_identity oi ON e.acl_object_identity = oi.id
WHERE oi.object_id_identity = '1'
AND e.sid = oi.owner_sid;
```

**Solution:**

Ensure `applyOwnership` is called on resource creation:
```java
@Transactional
public Project createProject(ProjectCreateRequest request) {
    Project project = projectRepository.save(/* ... */);
    
    // This must be called!
    aclPermissionService.applyOwnership(
        Project.class,
        project.getId(),
        currentUser.getUsername()
    );
    
    return project;
}
```

### Issue: Admin Cannot Override ACL

**Symptom:**
Even admin user gets 403 Forbidden.

**Diagnosis:**

Check security configuration:
```java
@Bean
public AclAuthorizationStrategy aclAuthorizationStrategy() {
    // Should allow ROLE_ADMIN to modify ACLs
    return new AclAuthorizationStrategyImpl(
        new SimpleGrantedAuthority("ROLE_ADMIN")
    );
}
```

**Solution:**

Option 1: Bypass ACL for admins in controller:
```java
@PreAuthorize("hasRole('ADMIN') or hasPermission(#id, 'Project', 'READ')")
public ResponseEntity<Project> getProject(@PathVariable Long id) {
    // ...
}
```

Option 2: Grant admin access to all resources:
```java
// In data initialization
aclPermissionService.grantToRole(
    Project.class,
    projectId,
    Role.ADMIN,
    permissionRegistry.ownerDefaults().toArray(new Permission[0])
);
```

---

## Inheritance Issues

### Issue: Child Not Inheriting Parent Permissions

**Symptom:**
User has permission on Project but cannot access Document in that Project.

**Diagnosis:**

1. **Check inheritance is enabled**
   ```sql
   SELECT oi.*, parent.object_id_identity as parent_id
   FROM acl_object_identity oi
   LEFT JOIN acl_object_identity parent ON oi.parent_object = parent.id
   WHERE oi.object_id_identity = '1'
   AND oi.object_id_class = (
       SELECT id FROM acl_class WHERE class = 'com.example.acl.domain.Document'
   );
   ```
   
   Check `entries_inheriting` is `true`.

2. **Check parent ACL exists**
   ```sql
   SELECT * FROM acl_object_identity WHERE id = <parent_id>;
   ```

**Solution:**

Ensure inheritance is set when creating child:
```java
// When creating document
Document document = documentRepository.save(/* ... */);

aclPermissionService.setParent(
    Document.class, document.getId(),
    Project.class, document.getProject().getId(),
    true  // entries inheriting = true!
);
```

### Issue: Wrong Parent Set

**Symptom:**
Document inherits from wrong Project.

**Diagnosis:**
```sql
SELECT 
    child.object_id_identity as document_id,
    parent.object_id_identity as project_id,
    child.entries_inheriting
FROM acl_object_identity child
JOIN acl_object_identity parent ON child.parent_object = parent.id
WHERE child.id = <document_oid>;
```

**Solution:**
Update parent:
```java
aclPermissionService.setParent(
    Document.class, documentId,
    Project.class, correctProjectId,
    true
);
```

---

## Cache Issues

### Issue: Changes Not Reflected Immediately

**Symptom:**
Permission granted but user still gets 403 until application restart.

**Cause:**
ACL cache not evicted after permission change.

**Solution:**

1. **Verify auto-eviction is working**
   
   Check `AclPermissionService.updateAcl()`:
   ```java
   private void updateAcl(MutableAcl acl) {
       aclService.updateAcl(acl);
       aclCache.evictFromCache(acl.getObjectIdentity());  // Should be here
   }
   ```

2. **Manual eviction**
   ```java
   aclPermissionService.evictCache(Project.class, projectId);
   ```

3. **Reduce cache TTL for development**
   ```java
   @Bean
   public EhCacheFactoryBean ehCacheAcl() {
       CacheConfiguration config = new CacheConfiguration()
           .timeToLiveSeconds(60)   // Shorter for dev
           .timeToIdleSeconds(30);
       // ...
   }
   ```

### Issue: Memory Issues with Cache

**Symptom:**
`OutOfMemoryError` or high memory usage.

**Solution:**

Reduce cache size:
```java
.maxEntriesLocalHeap(1000)  // Reduce from default 2048
```

Enable disk overflow:
```java
.maxEntriesLocalHeap(1000)
.overflowToDisk(true)
.maxEntriesLocalDisk(10000)
```

---

## Performance Issues

### Issue: Slow Permission Checks

**Diagnosis:**

1. **Enable query logging**
   ```properties
   logging.level.org.hibernate.SQL=DEBUG
   logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
   ```

2. **Check for N+1 queries**
   
   Look for repeated ACL lookups in logs.

**Solutions:**

1. **Use batch operations**
   ```java
   // Instead of
   for (Long id : projectIds) {
       aclPermissionService.grantToUser(Project.class, id, "bob", READ);
   }
   
   // Use
   aclPermissionService.bulkGrantToUsers(
       Project.class, projectIds, "bob", READ
   );
   ```

2. **Increase cache size**
   ```java
   .maxEntriesLocalHeap(5000)
   ```

3. **Add database indexes** (already in schema.sql)

4. **Use @PostFilter sparingly**
   
   `@PostFilter` loads all entities then filters. For large datasets:
   ```java
   // Instead of
   @PostFilter("hasPermission(filterObject, 'READ')")
   List<Project> findAll();
   
   // Use
   List<Project> findAccessibleByUser(String username);
   ```

### Issue: Slow Application Startup

**Cause:**
ACL initialization for many resources.

**Solution:**

1. **Lazy initialization**
   ```java
   // Create ACL on first access instead of at creation
   ```

2. **Batch ACL creation**
   ```java
   // Group multiple ACL operations in single transaction
   ```

3. **Reduce seed data**
   
   Remove unnecessary test data in data initialization.

---

## Database Issues

### Issue: H2 Console Not Accessible

**Symptom:**
http://localhost:8080/h2-console returns 404 or login fails.

**Solutions:**

1. **Check configuration**
   ```properties
   spring.h2.console.enabled=true
   spring.h2.console.path=/h2-console
   ```

2. **Check security configuration**
   ```java
   .authorizeHttpRequests(auth -> auth
       .requestMatchers("/h2-console/**").permitAll()
   )
   .headers(headers -> headers.frameOptions().sameOrigin())
   ```

3. **Correct JDBC URL**
   - JDBC URL: `jdbc:h2:mem:acldb`
   - Username: `sa`
   - Password: (empty)

### Issue: Data Lost After Restart

**Symptom:**
All data disappears when application restarts.

**Cause:**
H2 in-memory database by default.

**Solution for Persistence:**

```properties
# Use file-based H2
spring.datasource.url=jdbc:h2:file:./data/acldb;DB_CLOSE_ON_EXIT=FALSE

# Keep create-drop for development, use update for persistence
spring.jpa.hibernate.ddl-auto=update
```

### Issue: Foreign Key Constraint Violations

**Symptom:**
```
Referential integrity constraint violation
```

**Common Causes:**

1. **Deleting parent without cascade**
   
   Solution: Delete child ACLs first
   ```java
   // Delete child ACLs
   aclService.deleteAcl(childOid, true);
   // Then parent
   aclService.deleteAcl(parentOid, true);
   ```

2. **Orphaned ACL entries**
   
   Solution: Clean up
   ```sql
   -- Find orphaned entries
   SELECT * FROM acl_entry e
   WHERE NOT EXISTS (
       SELECT 1 FROM acl_object_identity oi
       WHERE oi.id = e.acl_object_identity
   );
   ```

---

## Configuration Issues

### Issue: Properties Not Loading

**Symptom:**
Custom properties in `application.properties` are ignored.

**Solutions:**

1. **Check file location**
   ```
   src/main/resources/application.properties  ✓
   src/main/resources/application.yml        ✓
   application.properties (project root)      ✗
   ```

2. **Check for typos**
   ```properties
   # Wrong
   spring.datasource.url=...
   
   # Correct (check exact property name)
   spring.datasource.url=...
   ```

3. **Profile-specific properties**
   ```bash
   # Run with specific profile
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Issue: Bean Creation Errors

**Symptom:**
```
Error creating bean with name 'aclConfig'
```

**Common Causes:**

1. **Circular dependency**
   
   Solution: Use `@Lazy` injection

2. **Missing dependency**
   
   Check `pom.xml` includes:
   ```xml
   <dependency>
       <groupId>org.springframework.security</groupId>
       <artifactId>spring-security-acl</artifactId>
   </dependency>
   ```

3. **Constructor injection order**
   
   Use `@RequiredArgsConstructor` with final fields

---

## Development Tips

### Enable SQL Logging

```properties
# Show SQL
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Show bind parameters
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Debug ACL Decisions

Add temporary logging:
```java
@Aspect
@Component
public class AclLoggingAspect {
    
    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object logAclCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("ACL check for: {}", joinPoint.getSignature());
        try {
            Object result = joinPoint.proceed();
            log.debug("ACL check passed");
            return result;
        } catch (AccessDeniedException e) {
            log.debug("ACL check failed: {}", e.getMessage());
            throw e;
        }
    }
}
```

### Query ACL Data Directly

```sql
-- Complete ACL info for a resource
SELECT 
    c.class as resource_type,
    oi.object_id_identity as resource_id,
    s.sid as subject,
    s.principal as is_user,
    e.mask as permission_mask,
    e.granting as is_granted
FROM acl_entry e
JOIN acl_object_identity oi ON e.acl_object_identity = oi.id
JOIN acl_class c ON oi.object_id_class = c.id
JOIN acl_sid s ON e.sid = s.id
WHERE oi.object_id_identity = '1'
ORDER BY e.ace_order;
```

### Reset ACL Data

```java
@Component
public class AclDataResetter {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void resetAclTables() {
        jdbcTemplate.execute("DELETE FROM acl_entry");
        jdbcTemplate.execute("DELETE FROM acl_object_identity");
        jdbcTemplate.execute("DELETE FROM acl_class");
        jdbcTemplate.execute("DELETE FROM acl_sid");
    }
}
```

### Test Without ACL

Temporarily bypass ACL for debugging:
```java
@PreAuthorize("permitAll()")  // Temporarily allow all
public ResponseEntity<Project> getProject(@PathVariable Long id) {
    // ...
}
```

---

## Getting Help

### Check Logs First

```bash
# Application logs
tail -f logs/application.log

# Spring Security logs
grep "Security" logs/application.log

# ACL-specific logs
grep "ACL" logs/application.log
```

### Enable Full Debug Mode

```properties
logging.level.root=DEBUG
```

### Common Log Messages

| Message | Meaning | Action |
|---------|---------|--------|
| `Access is denied` | Permission check failed | Check ACL entries |
| `NotFoundException` | ACL doesn't exist | Create ACL for resource |
| `Bad credentials` | Wrong username/password | Verify credentials |
| `Table 'acl_sid' doesn't exist` | Schema not initialized | Check SQL init config |

### Useful SQL Queries

See [Database Queries](ACL_SETUP.md#troubleshooting) in ACL Setup Guide.

### Community Resources

- [Spring Security ACL Documentation](https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html)
- [Spring Security Forum](https://github.com/spring-projects/spring-security/discussions)
- [Stack Overflow - Spring Security ACL](https://stackoverflow.com/questions/tagged/spring-security+acl)

---

## Quick Reference

### Permission Masks
```
READ            = 1
WRITE           = 2
CREATE          = 4
DELETE          = 8
ADMINISTRATION  = 16
SHARE           = 32
APPROVE         = 64
ALL             = 31 (R+W+C+D+A)
```

### Default Users
```
admin:admin123    (ADMIN)
alice:password123 (MANAGER)
bob:password123   (MEMBER)
carol:password123 (MEMBER)
dave:password123  (VIEWER)
```

### Key Endpoints
```
Health:       /actuator/health
H2 Console:   /h2-console
ACL Status:   /api/acl/status
Projects:     /api/projects
Documents:    /api/documents
Comments:     /api/comments
Permissions:  /api/permissions
```

---

**Last Updated:** 2024-01-15  
**Version:** 1.0.0
