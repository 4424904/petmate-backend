package com.petmate.domain.auth.service;

import com.petmate.common.repository.mybatis.token.RefreshTokenMapper;
import com.petmate.common.repository.mybatis.user.MemberMapper;
import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.TokenResponseDto;
import com.petmate.domain.auth.dto.response.MemberDto;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberMapper memberMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // 로그인
    public TokenResponseDto signin(LoginRequestDto request) {
        // 1) 회원 조회
        MemberDto member = memberMapper.findById(request.getId());
        if (member == null) throw new RuntimeException("존재하지 않는 아이디입니다.");

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(request.getPw(), member.getPw()))
            throw new RuntimeException("비밀번호가 올바르지 않습니다.");

        // 3) 토큰 발급 (JwtUtil + JwtClaimAccessor)
        String accessToken = jwtUtil.issue(
                member.getId(),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        List.of("USER"),      // 권한
                        "LOCAL",              // provider
                        member.getMail(),     // email
                        null,                 // nickname
                        null                  // picture
                )
        );

        String refreshToken = jwtUtil.issue(
                member.getId(),
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );

        // 4) 리프레시 저장
        refreshTokenMapper.saveToken(member.getNo(), refreshToken);

        // 5) 반환
        return new TokenResponseDto(accessToken, refreshToken);
    }

    // RefreshToken으로 AccessToken 재발급
    public TokenResponseDto refreshAccessToken(String refreshToken) {
        // 1) 만료/형식 검증
        if (jwtUtil.isExpired(refreshToken))
            throw new RuntimeException("RefreshToken이 만료되었습니다. 다시 로그인하세요.");

        Claims claims = jwtUtil.parse(refreshToken);
        if (!"refresh".equals(JwtClaimAccessor.type(claims)))
            throw new RuntimeException("유효하지 않은 토큰 유형입니다.");

        // (선택) 저장소 확인이 필요하면 여기서 refreshTokenMapper로 존재 여부 검증

        // 2) 주체 추출 및 새 액세스 발급
        String userId = claims.getSubject();

        String newAccessToken = jwtUtil.issue(
                userId,
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        List.of("USER"),
                        "LOCAL",
                        null,
                        null,
                        null
                )
        );

        return new TokenResponseDto(newAccessToken);
    }

    // 회원가입
    public void signup(SignupRequestDto request) {
        String encodedPw = passwordEncoder.encode(request.getPw());

        MemberDto member = MemberDto.builder()
                .id(request.getId())
                .pw(encodedPw)
                .mail(request.getMail())
                .build();

        memberMapper.signup(member);
    }

    // 로그아웃
    public void signout(String refreshToken) {
        // 저장된 리프레시 삭제
        refreshTokenMapper.deleteByToken(refreshToken);
    }
}
