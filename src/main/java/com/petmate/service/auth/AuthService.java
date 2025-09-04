package com.petmate.service.auth;

import com.petmate.dto.request.auth.LoginRequestDto;
import com.petmate.dto.request.auth.SignupRequestDto;
import com.petmate.dto.response.auth.TokenResponseDto;
import com.petmate.dto.MemberDto;
import com.petmate.repository.mybatis.user.MemberMapper;
import com.petmate.repository.mybatis.token.RefreshTokenMapper;
import com.petmate.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberMapper memberMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // 로그인
    public TokenResponseDto signin(LoginRequestDto request) {
        // 1. DB에서 회원 조회
        MemberDto member = memberMapper.findById(request.getId());
        if (member == null) {
            throw new RuntimeException("존재하지 않는 아이디입니다.");
        }

        // 2. 비밀번호 검증
        boolean matches = passwordEncoder.matches(request.getPw(), member.getPw());
        if (!matches) {
            throw new RuntimeException("비밀번호가 올바르지 않습니다.");
        }

        // 3. AccessToken 발급
        String accessToken = jwtUtil.generateToken(member.getId());

        // 4. RefreshToken 발급 + DB 저장
        String refreshToken = jwtUtil.generateRefreshToken(member.getId());
        refreshTokenMapper.saveToken(member.getNo(), refreshToken);

        // 5. AccessToken + RefreshToken 반환
        return new TokenResponseDto(accessToken, refreshToken);
    }

    // RefreshToken으로 AccessToken 재발급
    public TokenResponseDto refreshAccessToken(String refreshToken) {
        // 1. RefreshToken 만료 여부 체크
        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new RuntimeException("RefreshToken이 만료되었습니다. 다시 로그인하세요.");
        }

        // 2. 사용자 ID 추출
        String userId = jwtUtil.getUserId(refreshToken);

        // 3. 새 AccessToken 발급
        String newAccessToken = jwtUtil.generateToken(userId);

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

        refreshTokenMapper.deleteByToken(refreshToken);
    }
}
