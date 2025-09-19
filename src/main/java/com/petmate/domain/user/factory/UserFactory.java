package com.petmate.domain.user.factory;

import com.petmate.domain.user.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class UserFactory {

    /**
     * 새 UserEntity 생성
     * @param role   사용자 역할 ("1"=USER, "2"=PETOWNER, "3"=PETMATE, "4"=ALL, "9"=ADMIN)
     * @param status 상태 코드 (업무 정책에 따라 문자열 사용)
     */
    public UserEntity create(String email,
                             String name,
                             String nickName,
                             String provider,
                             String phone,
                             String role,
                             String status) {
        return UserEntity.builder()
                .email(email)
                .nickName(coalesce(nickName, name, "신규회원"))
                .name(coalesce(name, "신규회원"))
                .provider(normalize(provider))
                .profileImage(null)                 // 기본 이미지는 UserFileService에서 처리
                .phone(coalesce(phone, "010-0000-0000"))
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("N")
                .role(role)                         // 문자열 코드
                .mainService("9")
                .careSpecies("D")
                .status(status)                     // 문자열 코드
                .emailVerified("y")
                .build();
    }

    /**
     * 기존 UserEntity 정보 업데이트 (role, status는 변경하지 않음)
     */
    public void update(UserEntity u,
                       String name,
                       String nickName,
                       String phone,
                       String gender,
                       LocalDate birthDate,
                       String provider) {
        if (!blank(name))     u.setName(name);
        if (!blank(nickName)) u.setNickName(nickName);
        if (!blank(phone))    u.setPhone(phone);
        if (!blank(gender))   u.setGender(gender);
        if (birthDate != null) u.setBirthDate(birthDate);
        if (!blank(provider)) u.setProvider(normalize(provider));
    }

    private String normalize(String provider) {
        return blank(provider) ? "OAUTH2" : provider.toUpperCase(Locale.ROOT);
    }

    @SafeVarargs
    private static <T> T coalesce(T... values) {
        for (T v : values) {
            if (v instanceof String s) {
                if (!blank(s)) return v;
            } else if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
