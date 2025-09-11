// src/main/java/com/petmate/domain/user/service/UserService.java
package com.petmate.domain.user.service;

import com.petmate.common.storage.StorageService;
import com.petmate.domain.user.dto.request.PetmateApplyRequest;
import com.petmate.domain.user.entity.PetmateCertEntity;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.PetmateCertRepository;
import com.petmate.domain.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final StorageService storage;
    private final UserRepository userRepository;
    private final PetmateCertRepository certRepository;

    /** 펫메이트 신청 (파일 포함) */
    @Transactional
    public Integer apply(String email, PetmateApplyRequest req) {
        String prov  = blank(req.getProvider()) ? "OAUTH2" : req.getProvider().toUpperCase(Locale.ROOT);
        String nn    = !blank(req.getNickName()) ? req.getNickName()
                : !blank(req.getName())   ? req.getName()
                : "신규회원";
        String phone = blank(req.getPhone()) ? "010-0000-0000" : req.getPhone();

        // upsert by email
        UserEntity u = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                UserEntity.builder()
                        .email(email)
                        .nickName(nn)
                        .provider(prov)
                        .name(!blank(req.getName()) ? req.getName() : "신규회원")
                        .profileImage("profiles/default.png")
                        .phone(phone)
                        .birthDate(LocalDate.of(2000, 1, 1))
                        .gender(!blank(req.getGender()) ? req.getGender() : "N")
                        .role("3")            // 3: 펫메이트
                        .mainService("9")
                        .careSpecies("D")
                        .status("2")
                        .emailVerified("y")
                        .build()
        ));

        // update fields
        if (!blank(req.getName()))     u.setName(req.getName());
        if (!blank(req.getNickName())) u.setNickName(req.getNickName());
        if (!blank(req.getPhone()))    u.setPhone(req.getPhone());
        if (!blank(req.getGender()))   u.setGender(req.getGender());
        if (req.getAge() != null)      u.setBirthDate(LocalDate.now().minusYears(req.getAge()));
        if (!blank(req.getProvider())) u.setProvider(req.getProvider().toUpperCase(Locale.ROOT));

        // profile image store
        MultipartFile profile = req.getProfile();
        if (profile != null && !profile.isEmpty()) {
            String rel = storage.save(profile, "petmate/" + u.getId() + "/profile", "profile");
            u.setProfileImage(rel);
        }
        userRepository.save(u);

        // certificates store
        if (req.getCertificates() != null) {
            for (MultipartFile cert : req.getCertificates()) {
                if (cert == null || cert.isEmpty()) continue;
                String rel = storage.save(cert, "petmate/" + u.getId() + "/cert", "cert");
                certRepository.save(PetmateCertEntity.builder()
                        .userId(u.getId())
                        .filePath(rel)
                        .originalName(cert.getOriginalFilename())
                        .build());
            }
        }
        return u.getId();
    }

    /** 기본 유저 등록/업데이트 (파일 없음) */
    @Transactional
    public Integer applyBasicUser(String email,
                                  String provider,
                                  String name,
                                  String nickName,
                                  String phone,
                                  String gender,
                                  Integer age) {

        String prov = blank(provider) ? "OAUTH2" : provider.toUpperCase(Locale.ROOT);

        UserEntity u = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                UserEntity.builder()
                        .email(email)
                        .nickName(!blank(nickName) ? nickName : (!blank(name) ? name : "신규회원"))
                        .provider(prov)
                        .name(!blank(name) ? name : "신규회원")
                        .profileImage("profiles/default.png")
                        .phone(!blank(phone) ? phone : "010-0000-0000")
                        .birthDate(LocalDate.of(2000, 1, 1))
                        .gender(!blank(gender) ? gender : "N")
                        .role("1")            // 2: 반려인(기본 유저)
                        .mainService("9")
                        .careSpecies("D")
                        .status("1")
                        .emailVerified("y")
                        .build()
        ));

        if (!blank(name))     u.setName(name);
        if (!blank(nickName)) u.setNickName(nickName);
        if (!blank(phone))    u.setPhone(phone);
        if (!blank(gender))   u.setGender(gender);
        if (age != null)      u.setBirthDate(LocalDate.now().minusYears(age));
        if (!blank(provider)) u.setProvider(prov);

        userRepository.save(u);
        return u.getId();
    }

    @Transactional
    public Integer applyBasicUser(String email, String provider, String name, String nickName) {
        return applyBasicUser(email, provider, name, nickName, null, null, null);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
