package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.response.UserResponse;
import com.example.fooddelivery.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for converting User JPA entities into user-facing DTOs.
 */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a UserResponse DTO, securely omitting passwords.
     *
     * @param user the persistent User entity
     * @return UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
