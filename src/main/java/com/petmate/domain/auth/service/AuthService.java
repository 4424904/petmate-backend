// src/main/java/com/petmate/domain/auth/service/AuthService.java
package com.petmate.domain.auth.service;

import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.TokenResponseDto;
import com.petmate.domain.auth.dto.response.UserInfoResponseDto;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.public-img-url}")
    private String imgBase; // 예: http://localhost:8090/img/

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private String toImgUrl(String val){
        if (val == null || val.isBlank()) return null;
        if (val.startsWith("http://") || val.startsWith("https://")) return val;
        String base = imgBase.endsWith("/") ? imgBase : imgBase + "/";
        String file = val.startsWith("/") ? val.substring(1) : val;
        if (file.startsWith("img/")) file = file.substring(4);
        return base + file;
    }

    @Transactional
    public TokenResponseDto signin(String email) {
        // email 기준으로 JPA 조회
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        String role = (user.getRole() != null) ? user.getRole() : "1";

        // AccessToken 발급
        String accessToken = jwtUtil.issue(
                String.valueOf(user.getId()),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        user.getProvider(),
                        user.getEmail(),
                        user.getName(),
                        user.getNickName(),
                        user.getProfileImage(),
                        role,
                        user.getBirthDate() != null ? user.getBirthDate().toString() : null,
                        user.getGender(),
                        user.getPhone()
                )
        );

        // RefreshToken 발급
        String refreshToken = jwtUtil.issue(
                String.valueOf(user.getId()),
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }


    /** RefreshToken으로 AccessToken 재발급 (stateless) */
    public TokenResponseDto refreshAccessToken(String refreshToken) {
        if (jwtUtil.isExpired(refreshToken)) {
            throw new RuntimeException("RefreshToken이 만료되었습니다. 다시 로그인하세요.");
        }
        Claims claims = jwtUtil.parse(refreshToken);
        if (!"refresh".equals(JwtClaimAccessor.type(claims))) {
            throw new RuntimeException("유효하지 않은 토큰 유형입니다.");
        }

        Long uid = Long.parseLong(claims.getSubject());
        UserEntity user = userRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String role = user.getRole() != null ? user.getRole() : "1";

        String newAccessToken = jwtUtil.issue(
                String.valueOf(user.getId()),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        nz(user.getProvider(), "LOCAL"),
                        user.getEmail(),
                        user.getName(),
                        user.getNickName(),
                        toImgUrl(user.getProfileImage()),
                        role,
                        user.getBirthDate() != null ? user.getBirthDate().toString() : null,
                        user.getGender(),
                        user.getPhone()
                )
        );

        // 필요 시 refresh 회전(rotate) 로직 추가 가능
        return new TokenResponseDto(newAccessToken);
    }

    /** 회원가입 (간단 버전: email 중복 시 예외) */
    @Transactional
    public void signup(SignupRequestDto request) {
        if (userRepository.findByEmail(request.getMail()).isPresent()) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.getMail());
        user.setName(request.getId());          // 표시명으로 아이디 사용 (원하면 변경 가능)
        user.setNickName(request.getId());      // 기본 닉네임도 같이 설정
        user.setProvider("LOCAL");
        user.setRole("1");
        user.setStatus("2");                    // 활성 상태 기본값

        userRepository.save(user);
    }


    /** 로그아웃 (stateless: 서버 저장 refresh 미사용이므로 noop) */
    public void signout(String refreshToken) {
        // 서버 저장소를 쓰지 않으므로 특별히 삭제할 것은 없음.
        // 블랙리스트 등 상태 저장이 필요하면 여기서 구현.
    }

    @Transactional(readOnly = true)
    public UserInfoResponseDto getUserInfoByEmail(String email) {
        UserEntity u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        String birthDate = (u.getBirthDate()!=null)? u.getBirthDate().toString(): null;

        return new UserInfoResponseDto(
                String.valueOf(u.getId()),
                u.getEmail(),
                u.getName(),
                u.getNickName(),
                toImgUrl(u.getProfileImage()),
                u.getProvider(),
                u.getRole(),
                birthDate,
                u.getGender(),
                u.getPhone()
        );
    }

    private static String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
