package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.AuthRequest;
import com.ipca.dms_api.entity.User;
import com.ipca.dms_api.repository.UserRepository;
import com.ipca.dms_api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dmsApi/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String health() {
        return "DMS API is running";
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserId(),
                        request.getPassword()));

        // 2️⃣ Check user exists in DB
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not authorized in system"));

        // 3️⃣ Generate JWT
        String token = jwtTokenProvider.generateToken(user);

        return ResponseEntity.ok(Map.of("token", token));
    }

}