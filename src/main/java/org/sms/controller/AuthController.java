package org.sms.controller;

import org.sms.dto.AuthRequest;
import org.sms.dto.AuthResponse;
import org.sms.model.User;
import org.sms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        authService.registerUser(user.getUsername(), user.getPassword(), user.getEmail());
        return ResponseEntity.ok("User registered successfully");
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.authenticateUser(authRequest.getUsername(), authRequest.getPassword());
        return ResponseEntity.ok(response);
    }
}

