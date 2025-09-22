package com.petmate.domain.pet.service;

import com.petmate.domain.pet.dto.request.PetRequestDto;
import com.petmate.domain.pet.dto.response.PetResponseDto;
import com.petmate.domain.pet.entity.PetBreedEntity;
import com.petmate.domain.pet.entity.PetEntity;
import com.petmate.domain.pet.repository.jpa.PetBreedRepository;
import com.petmate.domain.pet.repository.jpa.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final PetBreedRepository breedRepository;
    private final PetImageService petImageService; // S3 업로드

    /** 반려동물 등록 */
    @Transactional
    public PetResponseDto createPet(PetRequestDto request, Long userId) {
        if (userId == null) throw new IllegalStateException("인증 정보가 없습니다.");
        log.info("반려동물 등록 시작 - 이름: {}, userId: {}", request.getName(), userId);

        PetEntity saved = petRepository.save(request.toEntity(userId));
        log.info("반려동물 등록 완료 - ID: {}, 이름: {}", saved.getId(), saved.getName());
        return PetResponseDto.from(saved);
    }

    /** species별 품종 목록 (id, name만) */
    public List<Map<String, Object>> findBreedsBySpecies(String speciesCode) {
        String code = speciesCode.trim().toUpperCase().substring(0, 1);
        List<PetBreedEntity> list = breedRepository.findBySpeciesOrderByNameAsc(code);
        return list.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            return m;
        }).collect(Collectors.toList());
    }

    /** 내 반려동물 전체 조회 */
    public List<PetResponseDto> getMyPetsByUserId(Long userId) {
        return petRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId)
                .stream().map(PetResponseDto::from).collect(Collectors.toList());
    }

    /** 내 반려동물 단건 조회 (소유권 검증 포함) */
    public PetResponseDto getMyPetById(Long userId, Long petId) {
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 권한이 없는 반려동물 ID: " + petId));
        return PetResponseDto.from(pet);
    }

    /** 반려동물 수정 */
    @Transactional
    public PetResponseDto updatePet(Long petId, PetRequestDto request, Long userId) {
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 반려동물이 존재하지 않거나 권한이 없습니다."));

        pet.updatePet(
                request.getName(),
                request.getImageUrl(),   // 프론트가 보낸 값이 있으면 그대로 반영(키 또는 절대URL)
                request.getSpecies(),
                request.getBreedId(),
                request.getGender(),
                request.getAgeYear(),
                request.getWeightKg(),
                request.getNeutered(),
                request.getTemper(),
                request.getNote()
        );
        log.info("반려동물 수정 완료 - ID={}, userId={}", petId, userId);
        return PetResponseDto.from(pet);
    }

    /** 반려동물 삭제 */
    @Transactional
    public void deletePet(Long petId, Long userId) {
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 반려동물이 존재하지 않거나 권한이 없습니다."));
        petRepository.delete(pet);
        log.info("반려동물 삭제 완료 - ID={}, userId={}", petId, userId);
    }

    /** 이미지 URL 직접 갱신(PATCH /pet/{id}/image 호환) */
    @Transactional
    public void updateImageUrl(Long petId, Long userId, String urlOrKey) {
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 권한이 없습니다."));
        pet.updateImageUrl(urlOrKey);
    }

    /** 이미지 업로드 + 즉시 키 반영(POST /pet/{id}/image 권장) */
    @Transactional
    public PetResponseDto uploadAndAttachImage(Long petId, Long userId, MultipartFile file) throws IOException {
        PetEntity pet = petRepository.findByIdAndOwnerUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 권한이 없습니다."));

        // ✅ S3 키만 반환받아 저장 (예: pets/38/uuid.png)
        String imageKey = petImageService.uploadPetMainImage(
                pet.getId().intValue(),
                file.getContentType(),
                file.getBytes()
        );
        pet.updateImageUrl(imageKey);  // DB에는 키 저장

        return PetResponseDto.from(pet);
    }
}
