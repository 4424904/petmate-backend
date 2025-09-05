package com.petmate.controller.auth;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private String userId;
    private String email;
    private String nickname;
    private String picture;
    private String provider;
    private List<String> roles; // 다중 권한 유지
}
