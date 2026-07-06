package com.fundmatrix.auth.service;

import com.fundmatrix.auth.domain.Role;
import com.fundmatrix.auth.domain.User;
import com.fundmatrix.auth.domain.UserStatus;
import com.fundmatrix.auth.dto.AuthResponse;
import com.fundmatrix.auth.dto.LoginRequest;
import com.fundmatrix.auth.dto.RegisterRequest;
import com.fundmatrix.auth.dto.UserDto;
import com.fundmatrix.auth.repository.UserRepository;
import com.fundmatrix.auth.security.JwtService;
import com.fundmatrix.commons.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = User.builder()
                .name(req.name())
                .email(req.email().toLowerCase())
                .phone(req.phone())
                .role(Role.INVESTOR)
                .status(UserStatus.ACTIVE)
                .password(passwordEncoder.encode(req.password()))
                .build();
        user = userRepository.save(user);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is " + user.getStatus());
        }
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.expiryFromNow(), toUserDto(user));
    }

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.getStatus());
    }
}
