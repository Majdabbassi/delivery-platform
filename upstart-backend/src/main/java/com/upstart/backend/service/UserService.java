package com.upstart.backend.service;

import com.upstart.backend.dto.UserResponseDto;
import com.upstart.backend.entity.User;
import com.upstart.backend.repository.UserRepository;
import com.upstart.backend.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Specification<User> spec = UserSpecification.hasUsernameOrEmail(usernameOrEmail);
        return userRepository.findOne(spec)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponseDto::fromUser);
    }
    
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findByUsernameDto(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponseDto::fromUser);
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponseDto::fromUser);
    }
    
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromUser)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UserResponseDto> getActiveUsers() {
        Specification<User> spec = UserSpecification.isEnabled(true);
        return userRepository.findAll(spec)
                .stream()
                .map(UserResponseDto::fromUser)
                .collect(Collectors.toList());
    }

    public UserResponseDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        user.setEnabled(!user.isEnabled());
        User updatedUser = userRepository.save(user);
        return UserResponseDto.fromUser(updatedUser);
    }
    
    // Specification-based search methods
    
    @Transactional(readOnly = true)
    public List<UserResponseDto> findUsersByRole(User.Role role) {
        Specification<User> spec = UserSpecification.hasRole(role);
        return userRepository.findAll(spec)
                .stream()
                .map(UserResponseDto::fromUser)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponseDto> searchUsers(String searchTerm, User.Role role, Boolean enabled, Pageable pageable) {
        Specification<User> spec = UserSpecification.buildSearchSpecification(searchTerm, role, enabled);
        return userRepository.findAll(spec, pageable)
                .map(UserResponseDto::fromUser);
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> findByUsernameOrEmail(String usernameOrEmail) {
        Specification<User> spec = UserSpecification.hasUsernameOrEmail(usernameOrEmail);
        return userRepository.findOne(spec)
                .map(UserResponseDto::fromUser);
    }
}