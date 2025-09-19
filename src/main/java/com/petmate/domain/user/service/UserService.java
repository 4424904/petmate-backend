package com.petmate.domain.user.service;

import com.petmate.domain.img.entity.ProfileImageMap;
import com.petmate.domain.img.repository.ProfileImageMapRepository;
import com.petmate.common.service.ImageService;
import com.petmate.domain.user.dto.request.PetmateApplyRequest;
import com.petmate.domain.user.dto.request.UserUpdateRequest;
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

import java.time.LocalDate;
import java.util.Map;
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

    // 상태 코드
    private static final String STATUS_DEFAULT = "1";
    private static final String STATUS_PETMATE = "2";

    // =========================
    // Role merge helpers
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
                                ROLE_PETMATE,
                                STATUS_PETMATE
                        )
                )
        );

        // 기본 정보 갱신(역할/상태 비변경) — birthDate 반영
        userFactory.update(
                user,
                req.getName(),
                req.getNickName(),
                req.getPhone(),
                req.getGender(),
                parseBirth(req.getBirthDate()),
                req.getProvider()
        );

        // 역할 병합
        String oldRole = user.getRole();
        String newRole = mergeToPetmate(oldRole);
        user.setRole(newRole);
        user.setStatus(STATUS_PETMATE);

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
        userFactory.update(
                user,
                req.getName(),
                req.getNickName(),
                req.getPhone(),
                req.getGender(),
                parseBirth(req.getBirthDate()),
                req.getProvider()
        );

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

        userRepository.save(user);
        log.info("프로필 등록/수정 완료 - userId={}, role({}->{})", user.getId(), currentRole, newRole);
        return user.getId();
    }

    /** 역할 계산 로직 */
    private String calculateNewRole(String currentRole, String targetRole) {
        if ("4".equals(targetRole)) return "4";
        if ("2".equals(currentRole) && "3".equals(targetRole)) return "4";
        if ("3".equals(currentRole) && "2".equals(targetRole)) return "4";
        if ("4".equals(currentRole)) return "4";
        return targetRole;
    }

    /** 기본 유저 생성/동기화 (소셜 로그인 시) */
    @Transactional
    public Long applyBasicUser(String email,
                               String provider,
                               String name,
                               String nickName,
                               String phone,
                               String gender,
                               String birthDate,
                               String profileImageUrl) {
        log.info("=== applyBasicUser 시작 === email={}, birthDate={}", email, birthDate);

        // 없으면 USER 생성(role=1, status=1)
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        userFactory.create(
                                email,
                                name,
                                nickName,
                                provider,
                                phone,
                                ROLE_USER,
                                STATUS_DEFAULT
                        )
                )
        );

        // 역할/상태는 되돌리지 않음
        userFactory.update(
                user,
                name,
                nickName,
                phone,
                gender,
                parseBirth(birthDate),
                provider
        );

        // 프로필 이미지 처리
        ensureSocialProfileImages(user.getEmail(), profileImageUrl);

        userRepository.save(user);
        log.info("applyBasicUser 완료 - userId={}, role={}", user.getId(), user.getRole());
        return user.getId();
    }

    /** 소셜 프로필 이미지 자동 저장 */
    private void ensureSocialProfileImages(String email, String socialImageUrl) {
        if (socialImageUrl == null || socialImageUrl.isBlank() || !socialImageUrl.startsWith("http")) {
            log.info("소셜 이미지 URL이 없음 - 자동 저장 스킵: email={}", email);
            return;
        }
        try {
            log.info("반려인 프로필 이미지(01) 체크 및 저장: email={}", email);
            imageService.getOrCreateSocialProfileImage("01", email, socialImageUrl);

            log.info("펫메이트 프로필 이미지(06) 체크 및 저장: email={}", email);
            imageService.getOrCreateSocialProfileImage("06", email, socialImageUrl);

            log.info("소셜 프로필 이미지 자동 저장 완료: email={}, url={}", email, socialImageUrl);
        } catch (Exception e) {
            log.error("소셜 프로필 이미지 자동 저장 실패: email={}, url={}", email, socialImageUrl, e);
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

        // 없으면 기본 생성(role=2, status=1)
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        userFactory.create(
                                email,
                                req.getName(),
                                req.getNickName(),
                                req.getProvider(),
                                req.getPhone(),
                                ROLE_PETOWNER,
                                STATUS_DEFAULT
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
                parseBirth(req.getBirthDate()),
                req.getProvider()
        );

        // 역할 병합
        String oldRole = user.getRole();
        String newRole = mergeToPetOwner(oldRole);
        user.setRole(newRole);

        // 프로필 이미지
        if (req.getProfile() != null && !req.getProfile().isEmpty()) {
            userFileService.storeProfile(user, req.getProfile());
        } else {
            userFileService.storeDefaultProfileIfAbsent(user);
        }

        userRepository.save(user);
        log.info("반려인 신청 완료 - userId={}, role(old->{})={}", user.getId(), oldRole, user.getRole());
        return user.getId();
    }

    // =========================
    // 내 정보 수정
    // =========================
    @Transactional
    public void updateMyInfo(String email, UserUpdateRequest req) {
        log.info("📌 내 정보 수정 요청 => email={}, req={}", email, req);
        log.info("📌 수신된 birthDate: '{}'", req.getBirthDate());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + email));

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }
        if (req.getNickName() != null && !req.getNickName().isBlank()) {
            user.setNickName(req.getNickName());
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            user.setPhone(req.getPhone());
        }

        if (req.getGender() != null && !req.getGender().isBlank()) {
            String g = req.getGender().trim().toUpperCase();
            if (g.startsWith("F") || g.startsWith("여")) g = "F";
            else if (g.startsWith("M") || g.startsWith("남")) g = "M";
            else g = "N";
            user.setGender(g);
        }

        if (req.getBirthDate() != null && !req.getBirthDate().isBlank()) {
            LocalDate birth = parseBirth(req.getBirthDate());
            if (birth != null) {
                user.setBirthDate(birth);
                log.info("✅ 생년월일 설정 완료: {}", birth);
            }
        }

        if (req.getProfileImageUrl() != null && !req.getProfileImageUrl().isBlank()) {
            user.setProfileImage(req.getProfileImageUrl());
        }

        userRepository.saveAndFlush(user);
        log.info("✅ 수정 반영 완료 - birthDate={}, gender={}, nickName={}",
                user.getBirthDate(), user.getGender(), user.getNickName());
    }

    /** ✅ 파일 포함 오버로드: pictureFile 저장까지 처리 */
    @Transactional
    public void updateMyInfo(String email, UserUpdateRequest req, MultipartFile pictureFile) {
        log.info("📌 내 정보 수정+파일 => email={}, birthDate={}", email, req.getBirthDate());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + email));

        // 필드 반영
        if (req.getName() != null && !req.getName().isBlank()) user.setName(req.getName());
        if (req.getNickName() != null && !req.getNickName().isBlank()) user.setNickName(req.getNickName());
        if (req.getPhone() != null && !req.getPhone().isBlank()) user.setPhone(req.getPhone());

        if (req.getGender() != null && !req.getGender().isBlank()) {
            String g = req.getGender().trim().toUpperCase();
            if (g.startsWith("F") || g.startsWith("여")) g = "F";
            else if (g.startsWith("M") || g.startsWith("남")) g = "M";
            else g = "N";
            user.setGender(g);
        }

        if (req.getBirthDate() != null && !req.getBirthDate().isBlank()) {
            LocalDate birth = parseBirth(req.getBirthDate());
            if (birth != null) user.setBirthDate(birth);
        }

        if (req.getProfileImageUrl() != null && !req.getProfileImageUrl().isBlank()) {
            user.setProfileImage(req.getProfileImageUrl());
        }

        // 파일이 있으면 저장하고 UUID 반영
        if (pictureFile != null && !pictureFile.isEmpty()) {
            String uuid = userFileService.storeProfile(user, pictureFile);
            user.setProfileImage(uuid);
            log.info("✅ 프로필 파일 저장 완료 uuid={}", uuid);
        }

        userRepository.saveAndFlush(user);
        log.info("✅ 수정+파일 반영 완료 - birthDate={}, gender={}, nickName={}",
                user.getBirthDate(), user.getGender(), user.getNickName());
    }

    @Transactional
    public void updateMyInfo(String email, String name, String nickName, String phone,
                             String gender, String birthDate, String profileImageUrl) {
        log.info("📌 내 정보 수정 요청 (오버로드) => email: {}, birthDate: {}", email, birthDate);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + email));

        if (name != null && !name.isBlank()) user.setName(name);
        if (nickName != null && !nickName.isBlank()) user.setNickName(nickName);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        if (gender != null && !gender.isBlank()) user.setGender(gender);

        if (birthDate != null && !birthDate.isBlank()) {
            LocalDate parsedDate = parseBirth(birthDate);
            if (parsedDate != null) {
                user.setBirthDate(parsedDate);
                log.info("✅ 오버로드 메서드 생년월일 설정: {}", parsedDate);
            }
        }

        if (profileImageUrl != null && !profileImageUrl.isBlank()) user.setProfileImage(profileImageUrl);

        userRepository.save(user);
        log.info("✅ 오버로드 메서드 수정 완료: {}", user.getNickName());
    }

    /** 회원 탈퇴 */
    @Transactional
    public void withdraw(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + email));
        user.setStatus("0");
        userRepository.save(user);
    }

    // UserService.java
    public Map<String,Object> findByEmail(String email){
        var u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + email));
        return Map.of(
                "userId", String.valueOf(u.getId()),
                "email", u.getEmail(),
                "name", u.getName(),
                "nickname", u.getNickName(),
                "gender", u.getGender(),
                "birthDate", u.getBirthDate()!=null? u.getBirthDate().toString():null,
                // ✅ 절대 URL로 교체
                "picture", findProfileImageByEmail(email),
                "provider", u.getProvider(),
                "role", u.getRole()
        );
    }


    // =========================
    // Utils
    // =========================
    private LocalDate parseBirth(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) return null;
        try {
            return LocalDate.parse(birthDate); // "YYYY-MM-DD"
        } catch (Exception e) {
            log.warn("❌ birthDate 파싱 실패: '{}' - {}", birthDate, e.getMessage());
            return null;
        }
    }
}
