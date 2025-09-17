package com.petmate.domain.img.service;

import com.petmate.domain.img.entity.ProfileImageMap;
import com.petmate.domain.img.repository.ProfileImageMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final ProfileImageMapRepository repo;

    /**
     * 이메일 기준으로 UUID가 있으면 재사용, 없으면 새로 생성 후 저장
     */
    public String getOrCreateUuidByEmail(String email, String pictureUrl) {
        return repo.findByEmail(email)
                .map(ProfileImageMap::getUuid)
                .orElseGet(() -> {
                    if (pictureUrl == null || pictureUrl.isBlank()) return null;

                    String ext = guessExt(pictureUrl);
                    String uuid = UUID.randomUUID().toString() + "." + ext;
                    String realPath = "C:/petmate/profile/" + uuid;

                    download(pictureUrl, realPath);

                    // ✅ Builder 사용 (createdAt, expiresAt 자동 처리)
                    repo.save(ProfileImageMap.builder()
                            .uuid(uuid)
                            .email(email)
                            .realPath(realPath)
                            .build());

                    return uuid;
                });
    }

    /**
     * UUID를 받아서 실제 로컬 경로 반환
     */
    public String resolveRealPath(String uuid) {
        log.info("UUID로 경로 조회 시작: {}", uuid);

        Optional<ProfileImageMap> result = repo.findByUuid(uuid);

        if (result.isPresent()) {
            String realPath = result.get().getRealPath();
            log.info("매핑 찾음: uuid={} -> path={}", uuid, realPath);
            return realPath;
        } else {
            log.warn("UUID에 해당하는 매핑을 찾을 수 없음: {}", uuid);

            // 디버깅: 전체 매핑 확인
            long totalCount = repo.count();
            log.info("전체 ProfileImageMap 레코드 수: {}", totalCount);

            return null;
        }
    }

    /**
     * 펫 이미지 UUID → 실제 로컬 경로 매핑
     * ex) uuid=abc.png → C:/petmate/{userId}/pet/abc.png
     */
    public String resolvePetImagePath(Long userId, String uuid) {
        if (uuid == null || uuid.isBlank()) {
            log.warn("잘못된 uuid 요청: {}", uuid);
            return null;
        }

        // 저장 규칙에 맞게 경로 생성
        String baseDir = "C:/petmate/" + userId + "/pet/";
        String realPath = baseDir + uuid;

        if (Files.exists(Paths.get(realPath))) {
            log.info("펫 이미지 경로 확인됨: {}", realPath);
            return realPath;
        } else {
            log.warn("펫 이미지 없음: {}", realPath);
            return null;
        }
    }

    /**
     * URL에서 파일 확장자 추출 (없으면 png 기본값)
     */
    private String guessExt(String url) {
        int i = url.lastIndexOf('.');
        return (i > 0) ? url.substring(i + 1).toLowerCase() : "png";
    }

    /**
     * URL에서 지정된 로컬 경로로 이미지 다운로드
     */
    private void download(String url, String path) {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 다운로드 실패", e);
        }
    }
}
