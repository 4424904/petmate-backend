package com.petmate.domain.pet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetFileService {

    @Value("${app.public-img-url}")
    private String publicImgUrl;

    /**
     * 반려동물 이미지 저장 후 접근 가능한 공개 URL 반환
     */
    public String savePetImage(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String baseDir = "c:/petmate/" + userId + "/pet/";
        Files.createDirectories(Path.of(baseDir));

        String original = file.getOriginalFilename();
        String ext = (original != null && original.lastIndexOf('.') > -1)
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "png";
        String filename = UUID.randomUUID() + "." + ext;

        Path savePath = Path.of(baseDir, filename);
        file.transferTo(savePath.toFile());

        // application.yml/app.properties에 지정된 publicImgUrl 기반으로 URL 생성
        return publicImgUrl + "pet/" + userId + "/" + filename;
    }
}
