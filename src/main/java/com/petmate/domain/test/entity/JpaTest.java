package com.petmate.domain.test.entity;

import com.petmate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jpa_test")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaTest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private JpaTestRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private JpaTestProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public JpaTest(String email, String name, String profileImage, 
                   JpaTestRole role, JpaTestProvider provider, String providerId) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 역할 변경 메서드
    public void changeRole(JpaTestRole role) {
        this.role = role;
    }

    // 프로필 업데이트 메서드
    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    // 계정 비활성화
    public void deactivate() {
        this.isActive = false;
    }
}