package com.petmate.domain.img.controller;

import com.petmate.domain.img.service.ProfileImageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/img")
public class ImageProxyController {

    private final ProfileImageService imageService;

    // ✅ uuid.png 등 확장자 포함 경로 허용
    @GetMapping("/{uuid:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String uuid) {
        log.info("이미지 요청: uuid={}", uuid);

        String path = imageService.resolveRealPath(uuid);
        log.info("매핑된 경로: {}", path);

        if (path == null) {
            log.warn("이미지 경로를 찾을 수 없음: {}", uuid);
            return ResponseEntity.notFound().build();
        }

        File file = new File(path);
        log.info("파일 존재 여부: {}, 경로: {}", file.exists(), file.getAbsolutePath());

        if (!file.exists()) {
            log.warn("실제 파일이 존재하지 않음: {}", path);

            // 디버깅: 디렉토리 내용 확인
            File parentDir = file.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                log.info("디렉토리 내용:");
                File[] files = parentDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        log.info("  - {}", f.getName());
                    }
                }
            }

            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        MediaType contentType = MediaType.IMAGE_PNG; // 기본값
        try {
            String mime = Files.probeContentType(Path.of(path));
            if (mime != null) contentType = MediaType.parseMediaType(mime);
            log.info("MIME 타입: {}", mime);
        } catch (Exception e) {
            log.warn("MIME 타입 판별 실패: {}", path);
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(resource);
    }

    @GetMapping("/pet/{userId}/{uuid:.+}")
    public ResponseEntity<Resource> servePetImage(
            @PathVariable Long userId,
            @PathVariable String uuid) {

        String path = imageService.resolvePetImagePath(userId, uuid); // ✅ userId 추가
        if (path == null) return ResponseEntity.notFound().build();

        File file = new File(path);
        Resource resource = new FileSystemResource(file);

        MediaType contentType = MediaType.IMAGE_PNG;
        try {
            String mime = Files.probeContentType(file.toPath());
            if (mime != null) contentType = MediaType.parseMediaType(mime);
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(resource);
    }


}
