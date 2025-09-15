package com.petmate.domain.user.service;

import com.petmate.domain.img.entity.ProfileImageMap;
import com.petmate.domain.img.repository.ProfileImageMapRepository;
import com.petmate.domain.user.entity.PetmateCertEntity;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.PetmateCertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFileService {

    @Value("${app.upload.dir:C:/petmate}")
    private String uploadRoot;

    private final PetmateCertRepository certRepository;
    private final ProfileImageMapRepository imageMapRepo;

    @Transactional
    public String storeProfile(UserEntity user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.info("업로드할 파일이 없음 - 기존 프로필 유지: {}", user.getProfileImage());
            return user.getProfileImage();
        }

        log.info("프로필 이미지 업로드 시작 - userId: {}, 기존 이미지: {}", user.getId(), user.getProfileImage());

        // 기존 UUID가 있으면 재사용, 없으면 새로 생성
        String existingUuid = user.getProfileImage();
        String uuid;

        if (existingUuid != null && !existingUuid.isBlank() &&
                !existingUuid.equals("profiles/default.png") && !existingUuid.equals("default.png")) {
            // 기존 UUID 재사용
            uuid = existingUuid;
            log.info("기존 UUID 재사용: {}", uuid);
        } else {
            // 새 UUID 생성
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            if (ext == null || ext.isBlank()) ext = "png";
            ext = ext.toLowerCase();
            uuid = UUID.randomUUID().toString() + "." + ext;
            log.info("새 UUID 생성: {}", uuid);
        }

        Path savePath = Paths.get(uploadRoot, String.valueOf(user.getId()), "profile", uuid);
        try {
            log.info("프로필 이미지 저장 시작: {}", savePath);
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath.toFile());
            log.info("프로필 이미지 저장 완료: {}", savePath);
        } catch (IOException e) {
            log.error("프로필 저장 실패: {}", savePath, e);
            throw new RuntimeException("프로필 저장 실패", e);
        }

        // ProfileImageMap 업데이트 (기존 UUID 유지)
        updateImageMapWithExistingUuid(user.getEmail(), uuid, savePath.toString());

        // user 테이블의 profile_image도 동일한 UUID로 설정
        user.setProfileImage(uuid);
        return uuid;
    }

    @Transactional
    public String storeProfileFromUrl(UserEntity user, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("이미지 URL이 비어있음: userId={}", user.getId());
            return storeDefaultProfileIfAbsent(user);
        }

        try {
            log.info("URL에서 프로필 이미지 저장 시작: userId={}, url={}", user.getId(), imageUrl);

            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();

                String ext = FilenameUtils.getExtension(imageUrl);
                if (ext == null || ext.isBlank()) ext = "png";
                ext = ext.toLowerCase();

                String uuid = UUID.randomUUID().toString() + "." + ext;

                Path savePath = Paths.get(uploadRoot, String.valueOf(user.getId()), "profile", uuid);
                log.info("파일 저장 경로: {}", savePath);

                Files.createDirectories(savePath.getParent());
                Files.write(savePath, bytes);

                log.info("프로필 이미지(URL) 저장 완료: {}", savePath);

                // ProfileImageMap 생성 (새 사용자)
                updateImageMapWithExistingUuid(user.getEmail(), uuid, savePath.toString());

                user.setProfileImage(uuid);
                return uuid;
            }
        } catch (Exception e) {
            log.error("프로필(URL) 저장 실패: userId={}, url={}", user.getId(), imageUrl, e);
            return storeDefaultProfileIfAbsent(user);
        }
    }

    @Transactional
    public String storeDefaultProfileIfAbsent(UserEntity user) {
        String cur = user.getProfileImage();

        boolean needsDefaultFile = (cur == null || cur.isBlank() ||
                "profiles/default.png".equals(cur) ||
                "default.png".equals(cur));

        if (!needsDefaultFile) {
            log.info("실제 프로필 이미지 존재: userId={}, profile={}", user.getId(), cur);
            return cur;
        }

        try {
            log.info("기본 프로필 이미지 저장 시작: userId={}, 현재값={}", user.getId(), cur);

            try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/profiles/default.png")) {
                if (is == null) {
                    log.error("default.png 리소스를 찾을 수 없음");
                    throw new IOException("default.png 리소스 없음");
                }

                byte[] bytes = is.readAllBytes();
                log.info("기본 이미지 리소스 읽기 완료: {} bytes", bytes.length);

                String uuid = UUID.randomUUID().toString() + ".png";
                Path savePath = Paths.get(uploadRoot, String.valueOf(user.getId()), "profile", uuid);
                log.info("기본 프로필 저장 경로: {}", savePath);

                Files.createDirectories(savePath.getParent());
                Files.write(savePath, bytes);

                log.info("기본 프로필 이미지 저장 완료: {}", savePath);

                updateImageMapWithExistingUuid(user.getEmail(), uuid, savePath.toString());

                user.setProfileImage(uuid);
                return uuid;
            }
        } catch (Exception e) {
            log.error("기본 프로필 저장 실패: userId={}", user.getId(), e);
            user.setProfileImage("default.png");
            return "default.png";
        }
    }

    /**
     * 기존 UUID를 유지하면서 ProfileImageMap 업데이트
     */
    @Transactional
    private void updateImageMapWithExistingUuid(String email, String uuid, String realPath) {
        ProfileImageMap existingMap = imageMapRepo.findByEmail(email).orElse(null);

        if (existingMap != null) {
            if (existingMap.getUuid().equals(uuid)) {
                // UUID가 같으면 경로만 업데이트
                log.info("기존 UUID 유지하며 경로만 업데이트: email={}, UUID={}, 새경로={}",
                        email, uuid, realPath);
                existingMap.setRealPath(realPath);
                imageMapRepo.save(existingMap);
            } else {
                // UUID가 다르면 삭제 후 재생성
                log.info("UUID 변경으로 인한 재생성: email={}, 기존UUID={} -> 새UUID={}",
                        email, existingMap.getUuid(), uuid);
                imageMapRepo.delete(existingMap);
                imageMapRepo.flush();

                imageMapRepo.save(ProfileImageMap.builder()
                        .uuid(uuid)
                        .email(email)
                        .realPath(realPath)
                        .build());
            }
        } else {
            // 새 레코드 생성
            log.info("새 ImageMap 생성: email={}, UUID={}", email, uuid);
            imageMapRepo.save(ProfileImageMap.builder()
                    .uuid(uuid)
                    .email(email)
                    .realPath(realPath)
                    .build());
        }

        log.info("ImageMap 처리 완료: email={}, UUID={}, path={}", email, uuid, realPath);
    }

    @Transactional
    public List<String> storeCertificates(UserEntity user, List<MultipartFile> files) {
        List<String> uuids = new ArrayList<>();
        if (files == null || files.isEmpty()) return uuids;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            if (ext == null || ext.isBlank()) ext = "png";
            ext = ext.toLowerCase();

            String uuid = UUID.randomUUID().toString() + "." + ext;
            uuids.add(uuid);

            Path savePath = Paths.get(uploadRoot, String.valueOf(user.getId()), "certs", uuid);
            try {
                log.info("자격증 파일 저장: {}", savePath);
                Files.createDirectories(savePath.getParent());
                file.transferTo(savePath.toFile());
                log.info("자격증 파일 저장 완료: {}", savePath);
            } catch (IOException e) {
                log.error("자격증 저장 실패: {}", savePath, e);
                throw new RuntimeException("자격증 저장 실패", e);
            }

            PetmateCertEntity cert = PetmateCertEntity.builder()
                    .userId(user.getId())
                    .uuidName(uuid)
                    .filePath(savePath.toString())
                    .originalName(file.getOriginalFilename())
                    .build();

            certRepository.save(cert);
        }

        return uuids;
    }
}