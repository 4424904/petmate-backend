package com.petmate.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String name;

    @JsonAlias({"nickname", "nick_name"})
    private String nickName;

    private String phone;

    // 'F','M','N' 등
    private String gender;

    // age 제거, birthDate로 통일
    private String birthDate; // "YYYY-MM-DD" 형식

    private String profileImageUrl;
}