package com.ipca.dms_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ipca.dms_api.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUserId(String userId);

    boolean existsByUserIdIgnoreCase(String userId);

    Optional<User> findByUserId(String userId);

    @Query("SELECT u FROM User u WHERE " +
            "UPPER(u.userId) LIKE %:search% OR " +
            "UPPER(u.firstName) LIKE %:search% OR " +
            "UPPER(u.lastName) LIKE %:search% OR " +
            "UPPER(u.locationName) LIKE %:search% OR " +
            "UPPER(u.departmentName) LIKE %:search%")
    Page<User> searchUsers(String search, Pageable pageable);

    Page<User> findByUserIdNotIgnoreCase(String userId, Pageable pageable);

    @Query("""
               SELECT u FROM User u
               WHERE UPPER(u.userId) <> 'ADMIN'
               AND (
                   UPPER(u.userId) LIKE %:search% OR
                   UPPER(u.firstName) LIKE %:search% OR
                   UPPER(u.lastName) LIKE %:search% OR
                   UPPER(u.locationName) LIKE %:search% OR
                   UPPER(u.departmentName) LIKE %:search%
               )
            """)
    Page<User> searchUsersExcludingAdmin(@Param("search") String search, Pageable pageable);

}
