package com.petmate.domain.user.service;

import com.petmate.domain.img.entity.ProfileImageMap;
import com.petmate.domain.img.repository.ProfileImageMapRepository;
import com.petmate.common.service.ImageService;
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
    private final ImageService imageService;

    // =========================
    // Role constants (String)
    // 1 USER, 2 PETOWNER, 3 PETMATE, 4 ALL, 9 ADMIN
    // =========================
    private static final String ROLE_USER = "1";
    private static final String ROLE_PETOWNER = "2";
    private static final String ROLE_PETMATE = "3";
    private static final String ROLE_ALL = "4";

    // 상태 코드도 문자열 사용 예) "1","2"
    private static final String STATUS_DEFAULT = "1";
    private static final String STATUS_PETMATE = "2";

    // =========================
    // Role merge helpers (String)
    // =========================
    private String mergeToPetmate(String current) {
        String r = (current == null || current.isBlank()) ? ROLE_USER : current;
        return switch (r) {
            case ROLE_USER -> ROLE_PETMATE;      // 1 -> 3
            case ROLE_PETOWNER -> ROLE_ALL;      // 2 -> 4
            case ROLE_PETMATE, ROLE_ALL -> r;    // 3,4 그대로
            default -> ROLE_PETMATE;             // 기타 -> 3
        };
    }

    private String mergeToPetOwner(String current) {
        String r = (current == null || current.isBlank()) ? ROLE_USER : current;
        return switch (r) {
            case ROLE_USER -> ROLE_PETOWNER;     // 1 -> 2
            case ROLE_PETMATE -> ROLE_ALL;       // 3 -> 4
            case ROLE_PETOWNER, ROLE_ALL -> r;   // 2,4 그대로
            default -> ROLE_PETOWNER;            // 기타 -> 2
        };
    }

    /** 펫메이트 신청 */
    @Transactional
    public Long apply(String email, PetmateApplyRequest req) {
        log.info("=== 펫메이트 신청 시작 === email={}", email);

        // 없으면 기본 생성(role=3, status=2)
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        userFactory.create(
                                email,
                                req.getName(),
                                req.getNickName(),
                                req.getProvider(),
                                req.getPhone(),
                                ROLE_PETMATE,     // role
                                STATUS_PETMATE    // status
                        )
                )
        );

        // 기본 정보 갱신(역할/상태 비변경)
        userFactory.update(
                user,
                req.getName(),
                req.getNickName(),
                req.getPhone(),
                req.getGender(),
                req.getAge(),
                req.getProvider()
        );

        // 역할 병합
        String oldRole = user.getRole();
        String newRole = mergeToPetmate(oldRole);
        user.setRole(newRole);
        user.setStatus(STATUS_PETMATE); // 정책에 맞게

        // 프로필 이미지
        if (req.getProfile() != null && !req.getProfile().isEmpty()) {
            userFileService.storeProfile(user, req.getProfile());
        } else {
            userFileService.storeDefaultProfileIfAbsent(user);
        }

        // 자격증 저장
        userFileService.storeCertificates(user, req.getCertificates());

        userRepository.save(user);
        log.info("펫메이트 신청 완료 - userId={}, role(old->{})={}", user.getId(), oldRole, user.getRole());
        return user.getId();
    }

    /** 통합 프로필 등록/수정 */
    @Transactional
    public Long applyProfile(String email, String targetRole, PetmateApplyRequest req) {
        log.info("=== applyProfile 시작 === email={}, targetRole={}", email, targetRole);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // 기본 정보 업데이트
        userFactory.update(user, req.getName(), req.getNickName(), req.getPhone(),
                          req.getGender(), req.getAge(), req.getProvider());

        // 역할 설정
        String currentRole = user.getRole();
        String newRole = calculateNewRole(currentRole, targetRole);
        user.setRole(newRole);

        // 상태 업데이트
        if ("3".equals(targetRole) || "4".equals(targetRole)) {
            user.setStatus(STATUS_PETMATE);
        } else {
            user.setStatus(STATUS_DEFAULT);
        }

        // 프로필 이미지는 ImageUploadViewer에서 별도 처리됨
        // (ImageService로 통일됨)

        // 자격증 (펫메이트만)
        // if ("3".equals(targetRole) || "4".equals(targetRole)) {
        //     userFileService.storeCertificates(user, req.getCertificates());
        // }

        userRepository.save(user);
        log.info("프로필 등록/수정 완료 - userId={}, role({}->{})", user.getId(), currentRole, newRole);
        return user.getId();
    }

    /** 역할 계산 로직 */
    private String calculateNewRole(String currentRole, String targetRole) {
        if ("4".equals(targetRole)) return "4"; // 명시적으로 둘다 요청

        if ("2".equals(currentRole) && "3".equals(targetRole)) return "4"; // 반려인 -> 펫메이트
        if ("3".equals(currentRole) && "2".equals(targetRole)) return "4"; // 펫메이트 -> 반려인
        if ("4".equals(currentRole)) return "4"; // 이미 둘다면 유지

        return targetRole; // 그 외에는 타겟 역할로
    }

    /** 기본 유저 생성/동기화 (소셜 로그인 시) */
    @Transactional
    public Long applyBasicUser(String email,
                                  String provider,
                                  String name,
                                  String nickName,
                                  String phone,
                                  String gender,
                                  Integer age,
                                  String profileImageUrl) {
        log.info("=== applyBasicUser 시작 === email={}", email);

        // 없으면 USER 생성(role=1, status=1)
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        userFactory.create(
                                email,
                                name,
                                nickName,
                                provider,
                                phone,
                                ROLE_USER,        // role
                                STATUS_DEFAULT    // status
                        )
                )
        );

        // 역할/상태는 되돌리지 않음
        userFactory.update(user, name, nickName, phone, gender, age, provider);

        // 프로필 이미지 처리 (ImageService 사용 - 01, 06 둘 다 자동 저장)
        ensureSocialProfileImages(user.getEmail(), profileImageUrl);

        userRepository.save(user);
        log.info("applyBasicUser 완료 - userId={}, role={}", user.getId(), user.getRole());
        return user.getId();
    }

    /**
     * 소셜 프로필 이미지 자동 저장 (01: 반려인, 06: 펫메이트 둘 다)
     * 묻지도 따지지도 말고 둘 다 체크해서 없으면 저장!
     */
    private void ensureSocialProfileImages(String email, String socialImageUrl) {
        if (socialImageUrl == null || socialImageUrl.isBlank() || !socialImageUrl.startsWith("http")) {
            log.info("소셜 이미지 URL이 없음 - 자동 저장 스킵: email={}", email);
            return;
        }

        try {
            // 01 (반려인 프로필) 체크 & 자동 저장
            log.info("반려인 프로필 이미지(01) 체크 및 저장: email={}", email);
            imageService.getOrCreateSocialProfileImage("01", email, socialImageUrl);

            // 06 (펫메이트 프로필) 체크 & 자동 저장
            log.info("펫메이트 프로필 이미지(06) 체크 및 저장: email={}", email);
            imageService.getOrCreateSocialProfileImage("06", email, socialImageUrl);

            log.info("소셜 프로필 이미지 자동 저장 완료: email={}, url={}", email, socialImageUrl);
        } catch (Exception e) {
            log.error("소셜 프로필 이미지 자동 저장 실패: email={}, url={}", email, socialImageUrl, e);
            // 실패해도 사용자 가입은 계속 진행
        }
    }

    /** 프로필 이미지 URL 조회 */
    public String findProfileImageByEmail(String email) {
        Optional<ProfileImageMap> imageMap = imageMapRepo.findByEmail(email);
        if (imageMap.isPresent()) {
            return imageBaseUrl + imageMap.get().getUuid();
        }
        String uuidPath = userRepository.findByEmail(email)
                .map(UserEntity::getProfileImage)
                .orElse(null);
        if (uuidPath == null || uuidPath.isBlank() || "default.png".equals(uuidPath)) {
            return imageBaseUrl + "profiles/default.png";
        }
        return imageBaseUrl + uuidPath;
    }

    /** 반려인 신청 */
    @Transactional
    public Long applyPetOwner(String email, PetmateApplyRequest req) {
        log.info("=== 반려인 신청 시작 === email={}", email);

        // 없으면 반려인 기본 생성(role=2, status=1)
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        userFactory.create(
                                email,
                                req.getName(),
                                req.getNickName(),
                                req.getProvider(),
                                req.getPhone(),
                                ROLE_PETOWNER,    // role
                                STATUS_DEFAULT    // status
                        )
                )
        );

        // 기본 정보 갱신
        userFactory.update(
                user,
                req.getName(),
                req.getNickName(),
                req.getPhone(),
                req.getGender(),
                req.getAge(),
                req.getProvider()
        );

        // 역할 병합
        String oldRole = user.getRole();
        String newRole = mergeToPetOwner(oldRole);
        user.setRole(newRole);
        // 필요 시 반려인 상태 규칙 적용
        // user.setStatus("1");

        // 프로필 이미지
        if (req.getProfile() != null && !req.getProfile().isEmpty()) {
            userFileService.storeProfile(user, req.getProfile());
        } else {
            userFileService.storeDefaultProfileIfAbsent(user);
        }

        // 반려인은 자격증 없음(무시)

        userRepository.save(user);
        log.info("반려인 신청 완료 - userId={}, role(old->{})={}", user.getId(), oldRole, user.getRole());
        return user.getId();
    }
}
