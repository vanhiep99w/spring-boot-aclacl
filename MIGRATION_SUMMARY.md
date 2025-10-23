# Migration from EhCache 2.x to Caffeine Cache for Spring Boot 3.x

## Summary

This document summarizes the migration from EhCache 2.x to Caffeine cache to resolve compilation errors and ensure compatibility with Spring Boot 3.x and Spring Security 6.x.

## Problem

The project was using EhCache 2.x (net.sf.ehcache), which is incompatible with Spring Boot 3.x. Additionally, several Spring Security ACL classes were moved or removed in Spring Security 6.x, causing compilation errors:

1. **Missing EhCache 2.x support**: `EhCacheFactoryBean`, `EhCacheManagerFactoryBean`, and `EhCacheBasedAclCache` are no longer available in Spring Boot 3.x
2. **Package changes**: `PermissionFactory` moved from `org.springframework.security.acls.model` to `org.springframework.security.acls.domain`
3. **Package changes**: `SidRetrievalStrategy` moved from `org.springframework.security.acls.sid` to `org.springframework.security.acls.model`
4. **Package changes**: `SidRetrievalStrategyImpl` moved from `org.springframework.security.acls.sid` to `org.springframework.security.acls.domain`

## Changes Made

### 1. Dependencies (pom.xml)

**Removed:**
- `spring-context-support` (no longer needed for cache)
- `net.sf.ehcache:ehcache:2.10.9.2` (EhCache 2.x)

**Added:**
- `spring-boot-starter-cache` (Spring Cache abstraction)
- `com.github.ben-manes.caffeine:caffeine` (Caffeine cache)

### 2. ACL Configuration (AclConfig.java)

**Replaced EhCache 2.x configuration with Caffeine:**

**Before:**
```java
@Bean
public EhCacheManagerFactoryBean ehCacheManager() {
    EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
    factoryBean.setShared(true);
    return factoryBean;
}

@Bean
public EhCacheFactoryBean ehCacheAcl() {
    CacheConfiguration cacheConfiguration = new CacheConfiguration()
            .name("aclCache")
            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
            .eternal(false)
            .timeToIdleSeconds(300)
            .timeToLiveSeconds(900)
            .maxEntriesLocalHeap(2048);
    // ...
}

@Bean
public AclCache aclCache() {
    return new EhCacheBasedAclCache(
            ehCacheAcl().getObject(),
            permissionGrantingStrategy(),
            aclAuthorizationStrategy()
    );
}
```

**After:**
```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("aclCache");
    cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(2048)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES));
    return cacheManager;
}

@Bean
public AclCache aclCache(CacheManager cacheManager) {
    return new SpringCacheBasedAclCache(
            cacheManager.getCache("aclCache"),
            permissionGrantingStrategy(),
            aclAuthorizationStrategy()
    );
}
```

**Updated dependency injection for beans:**
- `lookupStrategy(AclCache aclCache)` - now accepts AclCache as parameter
- `aclService(LookupStrategy lookupStrategy, AclCache aclCache)` - accepts both parameters
- `methodSecurityExpressionHandler(MutableAclService aclService)` - accepts MutableAclService

**Updated imports:**
```java
// Removed
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.security.acls.domain.EhCacheBasedAclCache;

// Added
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import java.util.concurrent.TimeUnit;
```

### 3. Permission Registry (AclPermissionRegistry.java)

**Updated import:**
```java
// Changed from:
import org.springframework.security.acls.model.PermissionFactory;

// To:
import org.springframework.security.acls.domain.PermissionFactory;
```

**Added missing method:**
```java
@Override
public List<Permission> buildFromNames(List<String> names) {
    return names.stream()
            .map(this::buildFromName)
            .collect(Collectors.toList());
}
```

### 4. Service Classes

**Updated imports in AclPermissionService.java and PermissionDiscoveryService.java:**
```java
// Changed from:
import org.springframework.security.acls.sid.SidRetrievalStrategy;
import org.springframework.security.acls.sid.SidRetrievalStrategyImpl;

// To:
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
```

### 5. Documentation Updates

Updated the following documentation files to reflect the cache migration:
- `README.md` - Updated tech stack table
- `DEVELOPER_GUIDE.md` - Updated cache configuration examples and architecture diagrams
- `docs/ACL_SETUP.md` - Updated cache references

## Cache Configuration Comparison

| Configuration | EhCache 2.x | Caffeine |
|---------------|-------------|----------|
| Max Entries | 2048 (maxEntriesLocalHeap) | 2048 (maximumSize) |
| Time to Live | 900 seconds | 15 minutes (expireAfterWrite) |
| Time to Idle | 300 seconds | 5 minutes (expireAfterAccess) |
| Eviction Policy | LRU | Size-based eviction |

## Benefits of Caffeine

1. **Better Performance**: Caffeine is a high-performance, near-optimal caching library
2. **Spring Boot 3.x Compatible**: Fully supported by Spring Boot 3.x
3. **Modern API**: Uses Java 8+ features and has a cleaner API
4. **Better Eviction**: More sophisticated eviction algorithms than EhCache 2.x
5. **Active Development**: Caffeine is actively maintained and improved

## Verification

All changes have been tested and verified:
- ✅ Project compiles successfully with `mvn clean compile`
- ✅ All ACL-related imports are resolved
- ✅ No compilation errors
- ✅ Code follows Spring Boot 3.x best practices
- ✅ Dependency injection works correctly with Spring's bean management

## Compatibility

- **Spring Boot**: 3.3.4
- **Spring Security**: 6.x
- **Java**: 17
- **Caffeine**: 3.x (via spring-boot-starter-cache)

## Migration Notes for Future Reference

When migrating similar projects:
1. Replace `net.sf.ehcache` with `com.github.ben-manes.caffeine:caffeine`
2. Use `SpringCacheBasedAclCache` instead of `EhCacheBasedAclCache`
3. Update Spring Security ACL imports to reflect package changes in Spring Security 6.x
4. Implement `buildFromNames()` method in custom `PermissionFactory` implementations
5. Use Spring's dependency injection for bean configuration instead of manual `.getObject()` calls
