package com.ipca.dms_api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ipca.dms_api.dto.PageResponse;
import com.ipca.dms_api.entity.User;
import com.ipca.dms_api.repository.UserRepository;
import com.ipca.dms_api.service.UserService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dmsApi/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @PostConstruct
    public void init() {
        System.out.println(">>> UserController LOADED");
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {

        String userId = authentication.getName();
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkUser(@RequestParam String userId) {
        return ResponseEntity.ok(userService.checkUser(userId));
    }

    @PostMapping("/createUser")
    public ResponseEntity<?> createUser(@RequestBody User user) {

        if (userRepository.existsByUserIdIgnoreCase(user.getUserId())) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/list")
    public PageResponse<User> userList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;

        if (search == null || search.trim().isEmpty()) {
            userPage = userRepository.findByUserIdNotIgnoreCase("admin", pageable);
        } else {
            userPage = userRepository.searchUsersExcludingAdmin(search.trim().toUpperCase(), pageable);
        }

        return new PageResponse<>(
                userPage.getContent(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isFirst(),
                userPage.isLast());
    }

}
