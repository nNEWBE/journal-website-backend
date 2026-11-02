package com.research.gbjournal.security;

import com.research.gbjournal.entity.User;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(buildAuthorities(user.getRole()))
                .disabled(!user.isEnabled())
                .build();
    }

    /**
     * Lookup used by the JWT filter on every authenticated request. Cached so a request
     * does not cost a database round-trip; every service that changes a user's email,
     * role, status or password evicts the "userDetails" cache.
     * The password hash is deliberately left out: it is never needed for token auth and
     * this instance is shared across requests.
     */
    @Cacheable(value = "userDetails", key = "#email.toLowerCase()")
    @Transactional(readOnly = true)
    public UserDetails loadUserForToken(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("")
                .authorities(buildAuthorities(user.getRole()))
                .disabled(!user.isEnabled())
                .build();
    }

    private Collection<GrantedAuthority> buildAuthorities(User.Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
