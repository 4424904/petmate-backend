// src/main/java/com/petmate/domain/user/dto/request/PetmateApplyRequest.java
package com.petmate.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter @Setter
public class PetmateApplyRequest {
    private String name;            // 이름
    private String nickName;        // 닉네임  ← 추가
    private String phone;           // 휴대폰   ← 추가
    private String gender;          // 성별
    private String birthDate;            // 나이
    private String provider;        // 소셜 제공자

    private MultipartFile profile;          // 프로필 단일
    private List<MultipartFile> certificates;   // 자격증 여러 개
}
