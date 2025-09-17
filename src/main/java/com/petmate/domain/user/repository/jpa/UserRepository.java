// src/main/java/com/petmate/domain/user/repository/jpa/UserRepository.java
package com.petmate.domain.user.repository.jpa;

import com.petmate.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

