package com.petmate.domain.user.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Comment;

import com.petmate.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 엔티티
 * 반려인, 펫메이트, 관리자 모든 사용자 정보를 관리
 */
@Entity
@Table(name = "USER")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id; // 사용자 ID (Primary Key)
    
    @Column(name = "EMAIL", nullable = false, unique = true, columnDefinition = "VARCHAR(255) COMMENT '이메일 (고유값)'")
    private String email;

    @Column(name = "ADDRESS", nullable = false, length = 255)
    @Comment("주소")
    private String address;

    @Column(name = "NICK_NAME", nullable = false, length = 80)
    @Comment("닉네임")
    private String nickName;

    @Column(name = "PROVIDER", nullable = false, length = 20)
    @Comment("소셜 로그인 제공자 (NAVER, KAKAO, GOOGLE)")
    private String provider; // 소셜 로그인 제공자 (NAVER, KAKAO, GOOGLE)

    @Column(name = "NAME", nullable = false, length = 80)
    @Comment("실명")
    private String name; // 실명

    @Column(name = "PROFILE_IMAGE", nullable = false, length = 500)
    @Comment("프로필 이미지")
    private String profileImage;
    
    @Column(name = "PHONE", nullable = false, length = 30)
    @Comment("휴대폰")
    private String phone;

    @Column(name = "BIRTH_DATE", nullable = false)
    @Comment("생년월일")
    private LocalDate birthDate;

    @Column(name = "GENDER", nullable = false, length = 1)
    @Comment("성별 (M:남성, F:여성)")
    private String gender;

    @Column(name = "ROLE", nullable = false, length = 1)
    @Comment("역할 (1:비회원, 2:반려인, 3:펫메이트, 4:반려인/펫메이트, 9:관리자)")
    private String role; // 역할 (1:비회원, 2:반려인, 3:펫메이트, 4:반려인/펫메이트, 9:관리자)

    @Column(name = "MAIN_SERVICE", nullable = false, length = 1)
    @Comment("주요 서비스 (1:돌봄, 2:산책, 3:미용, 4:병원, 9:기타)")
    private String mainService;
    
    @Column(name = "CARE_SPECIES", nullable = false, length = 1)
    @Comment("케어 가능 (D:강아지, C:고양이, O:기타)")
    private String careSpecies;
    
    @Column(name = "STATUS", nullable = false, length = 1)
    @Comment("상태 (1:대기, 2:활성, 3:탈퇴)")
    private String status; // 상태 (1:대기, 2:활성, 3:탈퇴)

    @Column(name = "EMAIL_VERIFIED", nullable = false, length = 1)
    @Comment("이메일 인증 여부")
    private String emailVerified;
    
    // created_at, updated_at은 BaseEntity에서 자동 관리됩니다!
}
