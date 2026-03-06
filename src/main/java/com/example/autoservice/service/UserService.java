package com.example.autoservice.service;

import com.example.autoservice.model.User;
import com.example.autoservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //минимум 8 символов, хотя бы одна цифра и одна буква
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User registerUser(String username, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (!isPasswordStrong(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters with at least one digit and one letter");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private boolean isPasswordStrong(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public boolean validatePasswordStrength(String password) {
        return isPasswordStrong(password);
    }
}