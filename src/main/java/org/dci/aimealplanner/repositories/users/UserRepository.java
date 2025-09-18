package org.dci.aimealplanner.repositories.users;

import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRole(Role role);
    Optional<User> findByVerificationToken(String verificationToken);
    long countByIsDeletedIsFalse();
    long countByEmailVerifiedTrueAndIsDeletedFalse();
    long countByEmailVerifiedFalseAndIsDeletedFalse();
    long countByIsDeletedTrue();

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActive(@Param("q") String q, Pageable pageable);


    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND u.userType = :type
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActiveByType(@Param("q") String q, @Param("type") UserType type, Pageable pageable);


    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND u.emailVerified = true
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActiveVerified(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND u.emailVerified = true
          AND u.userType = :type
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActiveVerifiedByType(@Param("q") String q, @Param("type") UserType type, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND u.emailVerified = false
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActiveUnverified(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = false
          AND u.emailVerified = false
          AND u.userType = :type
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchActiveUnverifiedByType(@Param("q") String q, @Param("type") UserType type, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = true
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchDeleted(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isDeleted = true
          AND u.userType = :type
          AND LOWER(u.email) LIKE :q
        """)
    Page<User> searchDeletedByType(@Param("q") String q, @Param("type") UserType type, Pageable pageable);
}
