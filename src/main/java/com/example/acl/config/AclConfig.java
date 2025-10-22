package com.example.acl.config;

import com.example.acl.repository.DocumentRepository;
import com.example.acl.repository.ProjectRepository;
import com.example.acl.repository.UserRepository;
import com.example.acl.security.CustomMethodSecurityExpressionHandler;
import com.example.acl.service.AclPermissionRegistry;
import lombok.RequiredArgsConstructor;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.EhCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class AclConfig {

    private final DataSource dataSource;
    private final AclPermissionRegistry permissionRegistry;

    // Repositories used by custom method security expressions
    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
    }

    @Bean
    public AclCache aclCache() {
        return new EhCacheBasedAclCache(
                ehCacheAcl().getObject(),
                permissionGrantingStrategy(),
                aclAuthorizationStrategy()
        );
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
        EhCacheFactoryBean factoryBean = new EhCacheFactoryBean();
        factoryBean.setCacheManager(ehCacheManager().getObject());
        factoryBean.setCacheConfiguration(cacheConfiguration);
        return factoryBean;
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheManager() {
        EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
        factoryBean.setShared(true);
        return factoryBean;
    }

    @Bean
    public LookupStrategy lookupStrategy() {
        return new BasicLookupStrategy(
                dataSource,
                aclCache(),
                aclAuthorizationStrategy(),
                new ConsoleAuditLogger()
        );
    }

    @Bean
    public MutableAclService aclService() {
        JdbcMutableAclService service = new JdbcMutableAclService(
                dataSource,
                lookupStrategy(),
                aclCache()
        );
        service.setClassIdentityQuery("SELECT @@IDENTITY");
        service.setSidIdentityQuery("SELECT @@IDENTITY");
        return service;
    }

    /**
     * Registers a custom MethodSecurityExpressionHandler that supports:
     * - Spring Security ACL checks via hasPermission/hasPermission(object, permission)
     * - Domain-specific helpers like isDocumentOwner(..) and hasProjectRole(..)
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        CustomMethodSecurityExpressionHandler expressionHandler = new CustomMethodSecurityExpressionHandler(
                documentRepository,
                projectRepository,
                userRepository
        );
        AclPermissionEvaluator permissionEvaluator = new AclPermissionEvaluator(aclService());
        permissionEvaluator.setPermissionFactory(permissionRegistry);
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        expressionHandler.setPermissionFactory(permissionRegistry);
        return expressionHandler;
    }
}
