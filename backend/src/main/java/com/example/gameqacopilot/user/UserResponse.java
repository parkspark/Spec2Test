package com.example.gameqacopilot.user;

import com.example.gameqacopilot.common.security.CurrentUser;

public record UserResponse(Long id, String email, String name, UserRole role) {

    public static UserResponse from(CurrentUser user) {
        return new UserResponse(user.id(), user.email(), user.name(), user.role());
    }
}
