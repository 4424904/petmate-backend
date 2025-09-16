package com.petmate.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 응답 DTO
 * - 프론트엔드에서 사용자 정보를 표시할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private String userId;     // 사용자 ID
    private String email;      // 이메일
    private String name;       // 이름
    private String nickname;   // 닉네임
    private String picture;    // 프로필 이미지 URL
    private String provider;   // 소셜 제공자 (KAKAO, NAVER, GOOGLE, OAUTH2 등)
    private String role;       // 역할 (1:USER, 2:PETOWNER, 3:PETMATE, 4:ALL, 9:ADMIN)

    // ✅ 추가 사용자 정보
    private String birthDate;  // 생년월일 (yyyy-MM-dd)
    private String gender;     // 성별 (M/F/N)
    private String phone;      // 전화번호
}
