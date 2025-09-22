package com.petmate.domain.pet.controller;

import com.petmate.domain.pet.dto.request.PetRequestDto;
import com.petmate.domain.pet.dto.response.PetResponseDto;
import com.petmate.domain.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** 내 반려동물 목록 */
    @GetMapping("/my")
    public ResponseEntity<List<PetResponseDto>> getMyPets(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = Long.parseLong(principal.getName()); // principal = userId
        List<PetResponseDto> pets = petService.getMyPetsByUserId(userId);
        return ResponseEntity.ok(pets);
    }

    /** 내 특정 반려동물 단건 */
    @GetMapping("/my/{petId}")
    public ResponseEntity<PetResponseDto> getMyPet(@PathVariable Long petId, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = Long.parseLong(principal.getName());
        PetResponseDto pet = petService.getMyPetById(userId, petId); // 소유권 검증 포함
        return ResponseEntity.ok(pet);
    }

    /** 반려동물 등록 */
    @PostMapping("/apply")
    public ResponseEntity<PetResponseDto> createPet(@Valid @RequestBody PetRequestDto request,
                                                    Principal principal) {
        if (principal == null) {
            log.warn("반려동물 등록 요청 거부 - 인증 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(principal.getName());
        log.info("반려동물 등록 요청 - 이름: {}, 종류: {}, userId: {}", request.getName(), request.getSpecies(), userId);
        PetResponseDto pet = petService.createPet(request, userId);
        return ResponseEntity.ok(pet);
    }

    /** species별 품종 조회: GET /pet/breeds?species=D */
    @GetMapping("/breeds")
    public ResponseEntity<?> getBreedsBySpecies(@RequestParam("species") String species) {
        if (species == null || species.isBlank()) {
            return ResponseEntity.badRequest().body("species is required");
        }
        String code = species.trim().toUpperCase().substring(0, 1);
        log.info(">>> /pet/breeds 호출 species={}, code={}", species, code);

        List<Map<String, Object>> breeds = petService.findBreedsBySpecies(code);
        log.info(">>> 조회된 breeds 개수={}", breeds.size());

        return ResponseEntity.ok(breeds);
    }

    /** 반려동물 수정 */
    @PutMapping("/{petId}")
    public ResponseEntity<PetResponseDto> updatePet(@PathVariable Long petId,
                                                    @Valid @RequestBody PetRequestDto request,
                                                    Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = Long.parseLong(principal.getName());
        PetResponseDto updated = petService.updatePet(petId, request, userId);
        return ResponseEntity.ok(updated);
    }

    /** 반려동물 삭제 */
    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable Long petId, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = Long.parseLong(principal.getName());
        petService.deletePet(petId, userId);
        return ResponseEntity.noContent().build();
    }
}
