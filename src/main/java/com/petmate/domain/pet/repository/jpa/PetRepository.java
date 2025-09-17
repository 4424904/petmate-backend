// ====================================
// repository/jpa/PetRepository.java
// ====================================
package com.petmate.domain.pet.repository.jpa;

import com.petmate.domain.pet.entity.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<PetEntity, Long> {

    /**
     * 특정 사용자의 반려동물 목록 조회
     */
    List<PetEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    /**
     * 특정 사용자의 특정 반려동물 조회 (권한 검증용)
     */
    Optional<PetEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    /**
     * 동물 종류별 조회
     */
    List<PetEntity> findByOwnerUserIdAndSpeciesOrderByCreatedAtDesc(Long ownerUserId, String species);

    /**
     * 이름으로 검색
     */
    List<PetEntity> findByOwnerUserIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long ownerUserId, String name);
}