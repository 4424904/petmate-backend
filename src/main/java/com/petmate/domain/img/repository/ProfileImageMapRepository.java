package com.petmate.domain.img.repository;

import com.petmate.domain.img.entity.ProfileImageMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileImageMapRepository extends JpaRepository<ProfileImageMap, String> {

    // ✅ 이메일로 기존 프로필 이미지 조회 (uuid, realPath 포함)
    Optional<ProfileImageMap> findByEmail(String email);

    // 🔥 UUID로 검색하는 메서드 추가
    Optional<ProfileImageMap> findByUuid(String uuid);
}
