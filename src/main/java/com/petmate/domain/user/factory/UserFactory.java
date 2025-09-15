package com.petmate.domain.user.factory;

import com.petmate.domain.user.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class UserFactory {

    public UserEntity create(String email, String name, String nickName, String provider,
                             String phone, String role, String status) {
        return UserEntity.builder()
                .email(email)
                .nickName(coalesce(nickName, name, "신규회원"))
                .name(coalesce(name, "신규회원"))
                .provider(normalize(provider))
                .profileImage(null)  // 🔥 수정: "profiles/default.png" → null
                .phone(coalesce(phone, "010-0000-0000"))
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("N")
                .role(role)
                .mainService("9")
                .careSpecies("D")
                .status(status)
                .emailVerified("y")
                .build();
    }

    public void update(UserEntity u,
                       String name,
                       String nickName,
                       String phone,
                       String gender,
                       Integer age,
                       String provider) {
        if (!blank(name))     u.setName(name);
        if (!blank(nickName)) u.setNickName(nickName);
        if (!blank(phone))    u.setPhone(phone);
        if (!blank(gender))   u.setGender(gender);
        if (age != null)      u.setBirthDate(LocalDate.now().minusYears(age));
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