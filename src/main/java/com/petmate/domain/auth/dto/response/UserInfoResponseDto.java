package com.petmate.domain.auth.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private String userId;
    private String email;
    private String name;      // 이 부분 추가
    private String nickname;
    private String picture;
    private String provider;
    private List<String> roles; // 다중 권한 유지
}
