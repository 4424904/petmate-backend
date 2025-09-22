package com.petmate.domain.pet.controller;

import com.petmate.domain.pet.service.PetFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class PetFileController {

    private final PetFileService petFileService;

    /** 반려동물 이미지 업로드 */
    @PostMapping("/pet")
    public ResponseEntity<Map<String, String>> uploadPetImage(
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = Long.parseLong(principal.getName());
        String url = petFileService.savePetImage(file, userId);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
