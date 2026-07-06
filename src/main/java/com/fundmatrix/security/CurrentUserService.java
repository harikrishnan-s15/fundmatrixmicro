package com.fundmatrix.security;

import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return user;
    }

    public Long getId() {
        return principal().getId();
    }

    public Role getRole() {
        return principal().getRole();
    }

//    public boolean hasRole(Role role) {
//        return getRole() == role;
//    }


    public User requireUser() {
        Long id = getId();
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
