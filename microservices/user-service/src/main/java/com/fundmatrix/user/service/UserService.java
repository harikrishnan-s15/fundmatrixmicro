package com.fundmatrix.user.service;

import com.fundmatrix.commons.exception.ResourceNotFoundException;
import com.fundmatrix.user.domain.User;
import com.fundmatrix.user.domain.UserStatus;
import com.fundmatrix.user.dto.CreateUserRequest;
import com.fundmatrix.user.dto.UpdateUserStatusRequest;
import com.fundmatrix.user.dto.UserDto;
import com.fundmatrix.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        return toUserDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));
        return toUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(com.fundmatrix.user.domain.Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::toUserDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(this::toUserDto)
                .toList();
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .role(req.role())
                .status(UserStatus.ACTIVE)
                .address(req.address())
                .city(req.city())
                .state(req.state())
                .zipcode(req.zipcode())
                .build();
        user = userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional
    public UserDto updateUserStatus(Long id, UpdateUserStatusRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        user.setStatus(req.status());
        user = userRepository.save(user);
        return toUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .toList();
    }

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getAddress(), user.getCity(),
                user.getState(), user.getZipcode());
    }
}
