package com.dung.ddmoney.controller;

import com.dung.ddmoney.config.JwtUtil;
import com.dung.ddmoney.dto.AuthRequest;
import com.dung.ddmoney.dto.AuthResponse;
import com.dung.ddmoney.dto.GoogleLoginRequest;
import com.dung.ddmoney.dto.RegisterRequest;
import com.dung.ddmoney.dto.UserResponse;
import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Incorrect email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        User user = userRepository.findByEmail(authRequest.getEmail()).get();

        if (!user.isEnabled()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account is disabled");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl());
        return ResponseEntity.ok(new AuthResponse(jwt, refreshToken, userResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: Email is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new user's account
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getEmail()); // Use email as username for simplicity
        user.setFullName(registerRequest.getFullName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            if (request.getIdToken() == null || request.getIdToken().trim().isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Token is empty");
                return ResponseEntity.badRequest().body(response);
            }

            User user = googleAuthService.verifyGoogleTokenAndGetUser(request.getIdToken());
            
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl());
            return ResponseEntity.ok(new AuthResponse(jwt, refreshToken, userResponse));
            
        } catch (RuntimeException e) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Paths.get("backend_error.log"), "RuntimeException: " + e.getMessage());
            } catch (Exception ex) {}
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            
            if (e.getMessage().contains("disabled")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Paths.get("backend_error.log"), "Exception: " + e.getMessage());
            } catch (Exception ex) {}
            Map<String, String> response = new HashMap<>();
            response.put("message", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
