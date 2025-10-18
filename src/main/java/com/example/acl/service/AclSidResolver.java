package com.example.acl.service;

import com.example.acl.domain.Group;
import com.example.acl.domain.Role;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class AclSidResolver {

    public PrincipalSid principalSid(String username) {
        return new PrincipalSid(username);
    }

    public GrantedAuthoritySid roleSid(Role role) {
        return new GrantedAuthoritySid("ROLE_" + role.name());
    }

    public GrantedAuthoritySid groupSid(Group group) {
        return new GrantedAuthoritySid("GROUP_" + group.name());
    }

    public GrantedAuthoritySid authoritySid(String authority) {
        return new GrantedAuthoritySid(authority);
    }

    public List<Sid> authenticationSids(Authentication authentication) {
        List<Sid> sids = new ArrayList<>();
        if (authentication == null) {
            return sids;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            sids.add(new PrincipalSid(userDetails.getUsername()));
        } else if (authentication.getName() != null) {
            sids.add(new PrincipalSid(authentication.getName()));
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities != null) {
            authorities.forEach(authority -> sids.add(new GrantedAuthoritySid(authority.getAuthority())));
        }
        return sids;
    }

    public List<Sid> currentAuthenticationSids() {
        return authenticationSids(SecurityContextHolder.getContext().getAuthentication());
    }
}
