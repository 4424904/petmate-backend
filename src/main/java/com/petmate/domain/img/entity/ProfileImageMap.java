package com.petmate.domain.img.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ 빌더 사용 가능하게 함
public class ProfileImageMap {

    @Id
    private String uuid; // 예: "abcd1234-...png"

    @Column(nullable = false, unique = true)
    private String email; // 이메일로 1:1 매핑

    @Column(name = "real_path", nullable = false)
    private String realPath; // 저장된 실제 경로

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
