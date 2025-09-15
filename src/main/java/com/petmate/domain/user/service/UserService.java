package com.petmate.domain.user.service;

import com.petmate.domain.img.entity.ProfileImageMap;
import com.petmate.domain.img.repository.ProfileImageMapRepository;
import com.petmate.domain.user.dto.request.PetmateApplyRequest;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.factory.UserFactory;
import com.petmate.domain.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.public-img-url}")
    private String imageBaseUrl;


    @Autowired
    private ProfileImageMapRepository imageMapRepo;

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final UserFileService userFileService;

    /** 펫메이트 신청 (파일 포함) */
    @Transactional
    public Integer apply(String email, PetmateApplyRequest req) {
        log.info("=== 펫메이트 신청 처리 시작 ===");
        log.info("email: {}, 새 프로필 파일 있음: {}", email, req.getProfile() != null && !req.getProfile().isEmpty());

        // 생성 or 조회
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("새 사용자 생성 (펫메이트 신청): {}", email);
            return userRepository.save(userFactory.create(email, req.getName(), req.getNickName(),
                    req.getProvider(), req.getPhone(), "3", "2"));
        });

        log.info("기존 사용자 조회 완료 - userId: {}, 현재 profileImage: {}", user.getId(), user.getProfileImage());

        // 갱신
        userFactory.update(user, req.getName(), req.getNickName(), req.getPhone(),
                req.getGender(), req.getAge(), req.getProvider());

        // 🔥 프로필 이미지 처리 개선
        if (req.getProfile() != null && !req.getProfile().isEmpty()) {
            log.info("새로운 프로필 이미지 파일이 업로드됨 - 기존 이미지 교체");
            String newUuid = userFileService.storeProfile(user, req.getProfile());
            log.info("새 프로필 이미지 저장 완료 - 새 UUID: {}", newUuid);
        } else {
            log.info("새 프로필 파일 없음 - 기존 프로필 이미지 유지: {}", user.getProfileImage());

            // 기존 프로필 이미지가 없는 경우에만 기본 이미지 설정
            if (user.getProfileImage() == null || user.getProfileImage().isBlank()) {
                log.info("기존 프로필도 없어서 기본 이미지 설정");
                String defaultUuid = userFileService.storeDefaultProfileIfAbsent(user);
                log.info("기본 이미지 설정 완료 - UUID: {}", defaultUuid);
            }
        }

        userRepository.save(user);
        userFileService.storeCertificates(user, req.getCertificates());

        log.info("펫메이트 신청 처리 완료 - 최종 profileImage: {}", user.getProfileImage());
        log.info("=== 펫메이트 신청 처리 완료 ===");

        return user.getId();
    }

    @Transactional
    public Integer applyBasicUser(String email,
                                  String provider,
                                  String name,
                                  String nickName,
                                  String phone,
                                  String gender,
                                  Integer age,
                                  String profileImageUrl) {

        log.info("=== applyBasicUser 시작 ===");
        log.info("email: {}, provider: {}, name: {}, nickname: {}, profileImageUrl: {}",
                email, provider, name, nickName, profileImageUrl);

        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("새 사용자 생성: {}", email);
            return userRepository.save(userFactory.create(email, name, nickName, provider, phone, "1", "1"));
        });

        log.info("사용자 조회/생성 완료 - userId: {}", user.getId());

        userFactory.update(user, name, nickName, phone, gender, age, provider);
        log.info("사용자 정보 업데이트 완료");

        // 🔥 여기가 핵심! 프로필 이미지 처리
        log.info("프로필 이미지 처리 시작 - profileImageUrl: {}", profileImageUrl);

        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            log.info("OAuth에서 받은 이미지 URL로 저장 시도: {}", profileImageUrl);
            String savedUuid = userFileService.storeProfileFromUrl(user, profileImageUrl);
            log.info("이미지 저장 완료 - UUID: {}", savedUuid);
        } else {
            log.info("이미지 URL이 없어서 기본 이미지 저장");
            String savedUuid = userFileService.storeDefaultProfileIfAbsent(user);
            log.info("기본 이미지 저장 완료 - UUID: {}", savedUuid);
        }

        userRepository.save(user);
        log.info("사용자 저장 완료 - 최종 profileImage: {}", user.getProfileImage());
        log.info("=== applyBasicUser 완료 ===");

        return user.getId();
    }


    public String findProfileImageByEmail(String email) {
        log.info("프로필 이미지 조회: {}", email);

        // 🔥 profile_image_map 테이블에서 직접 조회
        Optional<ProfileImageMap> imageMap = imageMapRepo.findByEmail(email);

        if (imageMap.isPresent()) {
            String uuid = imageMap.get().getUuid();
            log.info("profile_image_map에서 UUID 조회: {}", uuid);
            String fullUrl = imageBaseUrl + uuid;
            log.info("최종 URL: {}", fullUrl);
            return fullUrl;
        }

        // 🔥 fallback: user 테이블에서 조회 (기존 로직)
        String uuidPath = userRepository.findByEmail(email)
                .map(UserEntity::getProfileImage)
                .orElse(null);

        log.info("user 테이블에서 조회된 UUID: {}", uuidPath);

        if (uuidPath == null || uuidPath.isBlank() || "default.png".equals(uuidPath)) {
            log.info("UUID가 없거나 기본값이므로 기본 경로 반환");
            return imageBaseUrl + "profiles/default.png";
        }

        String fullUrl = imageBaseUrl + uuidPath;
        log.info("최종 URL (fallback): {}", fullUrl);
        return fullUrl;
    }
}