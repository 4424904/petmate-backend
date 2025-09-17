package com.petmate.domain.pet.service;

import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
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

    private final UserRepository userRepository;

    @Value("${public-img-url:http://localhost:8090/img/}")
    private String publicImgUrl;

    /** 이미지 저장 후 공개 URL 반환 */
    public String savePetImage(MultipartFile file, String userEmail) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        Long userId = userRepository.findByEmail(userEmail)
                .map(UserEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + userEmail));

        String baseDir = "c:/petmate/" + userId + "/pet/";
        Files.createDirectories(Path.of(baseDir));

        String original = file.getOriginalFilename();
        String ext = (original != null && original.lastIndexOf('.') > -1)
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "png";
        String filename = UUID.randomUUID() + "." + ext;

        Path savePath = Path.of(baseDir, filename);
        file.transferTo(savePath.toFile());

        // ✅ 절대 URL 반환 (application.yml 값 활용)
        return publicImgUrl + "pet/" + userId + "/" + filename;
    }
}
