package com.example.gameqacopilot.common.security;

import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CurrentUser(Long id, String email, String password, String name, UserRole role)
        implements UserDetails {

    static CurrentUser from(User user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getPassword(), user.getName(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
