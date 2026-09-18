package com.agenticai.controller;

import com.agenticai.controller.dto.LoginRequest;
import com.agenticai.model.User;
import com.agenticai.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody LoginRequest req) {
        if (req.getUsername() != null && req.getPassword() != null) {
            Optional<User> existingUser = userRepository.findByUsername(req.getUsername());
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", "Username already exists or creation failed"));
            }

            User newUser = new User(req.getUsername(), req.getPassword());
            userRepository.save(newUser);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Signup successful");
            response.put("user", Map.of("username", req.getUsername()));
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", "Missing username or password"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getUsername() != null && req.getPassword() != null) {
            Optional<User> user = userRepository.findByUsername(req.getUsername());
            if (user.isPresent() && user.get().getPassword().equals(req.getPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Login successful");
                response.put("user", Map.of("username", req.getUsername()));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid username or password"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", "Missing username or password"));
    }
}
