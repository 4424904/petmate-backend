package com.petmate.domain.pet.service;

import com.petmate.domain.pet.dto.request.PetRequestDto;
import com.petmate.domain.pet.dto.response.PetResponseDto;
import com.petmate.domain.pet.entity.PetBreedEntity;
import com.petmate.domain.pet.entity.PetEntity;
import com.petmate.domain.pet.repository.jpa.PetBreedRepository;
import com.petmate.domain.pet.repository.jpa.PetRepository;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetBreedRepository breedRepository; // 품종 조회용

    /** 반려동물 등록 */
    @Transactional
    public PetResponseDto createPet(PetRequestDto request, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        log.info("반려동물 등록 시작 - 이름: {}, 사용자: {}", request.getName(), userEmail);

        Long ownerId = getUserIdByEmail(userEmail);

        PetEntity petEntity = request.toEntity(ownerId);
        PetEntity saved = petRepository.save(petEntity);

        log.info("반려동물 등록 완료 - ID: {}, 이름: {}", saved.getId(), saved.getName());
        return PetResponseDto.from(saved);
    }

    /** species별 품종 목록 (id, name만) */
    public List<Map<String, Object>> findBreedsBySpecies(String speciesCode) {
        String code = speciesCode.trim().toUpperCase().substring(0,1);
        log.debug(">>> findBreedsBySpecies 요청 code={}", code);

        List<PetBreedEntity> list = breedRepository.findBySpeciesOrderByNameAsc(code);
        log.debug(">>> breedRepository 결과 count={}, data={}", list.size(), list);

        return list.stream()
                .map(b -> {
                    Map<String,Object> m = new HashMap<>();
                    m.put("id", b.getId());
                    m.put("name", b.getName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 이메일로 사용자 PK 조회 */
    private Long getUserIdByEmail(String email) {
        log.debug("사용자 ID 조회 - 이메일: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + email));
        return (long) user.getId();
    }

    // PetService.javA
    /** 내 반려동물 전체 조회 */
    public List<PetResponseDto> getMyPetsByEmail(String email) {
        Long ownerId = getUserIdByEmail(email);
        List<PetEntity> pets = petRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerId);
        return pets.stream()
                .map(PetResponseDto::from)
                .collect(Collectors.toList());
    }

    /** 내 반려동물 단건 조회 (소유권 검증 포함) */
    public PetResponseDto getMyPetById(String email, Long petId) {
        Long ownerId = getUserIdByEmail(email);
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 권한이 없는 반려동물 ID: " + petId));
        return PetResponseDto.from(pet);
    }

    @Transactional
    public PetResponseDto updatePet(Long petId, PetRequestDto request, String userEmail) {
        Long ownerId = getUserIdByEmail(userEmail);

        // 본인 소유 확인
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 반려동물이 존재하지 않거나 권한이 없습니다."));

        // PetEntity 내장 메서드 호출
        pet.updatePet(
                request.getName(),
                request.getImageUrl(),
                request.getSpecies(),
                request.getBreedId(),
                request.getGender(),
                request.getAgeYear(),
                request.getWeightKg(),
                request.getNeutered(),
                request.getTemper(),
                request.getNote()
        );

        log.info("반려동물 수정 완료 - ID={}, 사용자={}", petId, userEmail);
        return PetResponseDto.from(pet);
    }

    @Transactional
    public void deletePet(Long petId, String userEmail) {
        Long ownerId = getUserIdByEmail(userEmail);

        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 반려동물이 존재하지 않거나 권한이 없습니다."));

        petRepository.delete(pet);
        log.info("반려동물 삭제 완료 - ID={}, 사용자={}", petId, userEmail);
    }
}
