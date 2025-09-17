// src/main/java/com/petmate/domain/user/controller/UserController.java
package com.petmate.domain.user.controller;

import com.petmate.domain.user.dto.request.PetmateApplyRequest;
import com.petmate.domain.user.service.UserService;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 펫메이트 등록 (프로필/자격증 파일 포함) */
    @PostMapping(value = "/petmate/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyPetMate(
            @RequestParam("email") @Email String email,
            @ModelAttribute PetmateApplyRequest req
    ) {
        Long userId = userService.apply(email, req);
        return ResponseEntity.ok("펫메이트 신청 완료! 사용자 ID: " + userId);
    }

    @PostMapping(value = "/petowner/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyPetOwner(
            @RequestParam("email") @Email String email,
            @ModelAttribute PetmateApplyRequest req   // DTO 통일 사용
    ) {
        Long userId = userService.applyPetOwner(email, req);
        return ResponseEntity.ok("반려인 신청 완료! 사용자 ID: " + userId);
    }

    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> applyUser(
            @RequestParam("email") @Email String email,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "nickName", required = false) String nickName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "age", required = false) Integer age,
            @RequestParam(value = "profileImageUrl", required = false) String profileImageUrl // ✅ 추가
    ) {
        Long userId = userService.applyBasicUser(email, provider, name, nickName, phone, gender, age, profileImageUrl);
        return ResponseEntity.ok("유저 등록 완료! 사용자 ID: " + userId);
    }

}
