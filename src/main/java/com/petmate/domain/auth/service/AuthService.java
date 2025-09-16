// src/main/java/com/petmate/domain/auth/service/AuthService.java
package com.petmate.domain.auth.service;

import com.petmate.common.repository.mybatis.token.RefreshTokenMapper;
import com.petmate.common.repository.mybatis.user.MemberMapper;
import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.MemberDto;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.public-img-url}")
    private String imgBase; // 예: http://localhost:8090/img/
    // MyBatis: ID/PW 로그인·리프레시 용
    private final MemberMapper memberMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    // JPA: /auth/me 최신값 조회 용
    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private String toImgUrl(String val){
        if (val == null || val.isBlank()) return null;
        if (val.startsWith("http://") || val.startsWith("https://")) return val;
        String base = imgBase.endsWith("/") ? imgBase : imgBase + "/";
        String file = val.startsWith("/") ? val.substring(1) : val;
        // val이 'img/uuid.png' 같은 상대경로여도 정상 처리
        if (file.startsWith("img/")) file = file.substring(4);
        return base + file;
    }
    /** 로그인 */
    public TokenResponseDto signin(LoginRequestDto request) {
        MemberDto member = memberMapper.findById(request.getId());
        if (member == null) throw new RuntimeException("존재하지 않는 아이디입니다.");
        if (!passwordEncoder.matches(request.getPw(), member.getPw()))
            throw new RuntimeException("비밀번호가 올바르지 않습니다.");

        // MEMBER 테이블에 ROLE 컬럼 없음 → 기본값 고정
        String role = "1";

        String accessToken = jwtUtil.issue(
                member.getId(),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        "LOCAL",
                        nz(member.getMail(), null),
                        null, null, null,
                        role,
                        null, null, null
                )
        );
        String refreshToken = jwtUtil.issue(
                member.getId(),
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );

        refreshTokenMapper.saveToken(member.getNo(), refreshToken);
        return new TokenResponseDto(accessToken, refreshToken);
    }

    /** RefreshToken으로 AccessToken 재발급 */
    public TokenResponseDto refreshAccessToken(String refreshToken) {
        if (jwtUtil.isExpired(refreshToken))
            throw new RuntimeException("RefreshToken이 만료되었습니다. 다시 로그인하세요.");
        Claims claims = jwtUtil.parse(refreshToken);
        if (!"refresh".equals(JwtClaimAccessor.type(claims)))
            throw new RuntimeException("유효하지 않은 토큰 유형입니다.");

        String userId = claims.getSubject();
        MemberDto member = memberMapper.findById(userId);
        if (member == null) throw new RuntimeException("사용자를 찾을 수 없습니다.");

        String role = "1"; // 같은 이유로 고정

        String newAccessToken = jwtUtil.issue(
                userId,
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        "LOCAL",
                        nz(member.getMail(), null),
                        null, null, null,
                        role,
                        null, null, null
                )
        );
        return new TokenResponseDto(newAccessToken);
    }

    /** 회원가입 */
    public void signup(SignupRequestDto request) {
        String encodedPw = passwordEncoder.encode(request.getPw());
        MemberDto member = MemberDto.builder()
                .id(request.getId())
                .pw(encodedPw)
                .mail(request.getMail())
                .build();
        memberMapper.signup(member);
    }

    /** 로그아웃 */
    public void signout(String refreshToken) {
        refreshTokenMapper.deleteByToken(refreshToken);
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
                toImgUrl(u.getProfileImage()),  // <- 절대 URL로 변환
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
