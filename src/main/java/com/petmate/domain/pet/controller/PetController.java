package com.petmate.domain.pet.controller;

import com.petmate.domain.pet.dto.request.PetRequestDto;
import com.petmate.domain.pet.dto.response.PetResponseDto;
import com.petmate.domain.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** 내 반려동물 목록 */
    @GetMapping("/my")
    public ResponseEntity<List<PetResponseDto>> getMyPets(Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<PetResponseDto> pets = petService.getMyPetsByUserId(userId);
        return ResponseEntity.ok(pets);
    }

    /** 내 특정 반려동물 단건 */
    @GetMapping("/my/{petId}")
    public ResponseEntity<PetResponseDto> getMyPet(@PathVariable Long petId, Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        PetResponseDto pet = petService.getMyPetById(userId, petId); // 소유권 검증 포함
        return ResponseEntity.ok(pet);
    }

    /** 반려동물 등록 */
    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PetResponseDto> createPet(@Valid @RequestBody PetRequestDto request,
                                                    Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) {
            log.warn("반려동물 등록 요청 거부 - 인증 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
        // 허용 코드 검증(백엔드 DTO 검증 규칙과 일치)
        Set<String> allowed = Set.of("D","C","R","S","H","B","P","F","O");
        if (!allowed.contains(code)) {
            return ResponseEntity.badRequest().body("invalid species code");
        }

        log.info(">>> /pet/breeds 호출 species={}, code={}", species, code);
        List<Map<String, Object>> breeds = petService.findBreedsBySpecies(code);
        log.info(">>> 조회된 breeds 개수={}", breeds.size());
        return ResponseEntity.ok(breeds);
    }

    /** 반려동물 수정 */
    @PutMapping(value = "/{petId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PetResponseDto> updatePet(@PathVariable Long petId,
                                                    @Valid @RequestBody PetRequestDto request,
                                                    Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        PetResponseDto updated = petService.updatePet(petId, request, userId);
        return ResponseEntity.ok(updated);
    }

    /** 반려동물 삭제 */
    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable Long petId, Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        petService.deletePet(petId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 이미지 URL 직접 갱신(호환용) */
    @PatchMapping("/{petId}/image")
    public ResponseEntity<Void> updateImage(@PathVariable Long petId,
                                            @RequestBody Map<String, String> body,
                                            Principal principal) {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String url = body.get("imageUrl");
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().build();
        petService.updateImageUrl(petId, userId, url);
        return ResponseEntity.noContent().build();
    }

 @PostMapping(path = "/{petId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<PetResponseDto> uploadImage(@PathVariable Long petId,
                                                   @RequestParam("file") MultipartFile file,
                                                   Principal principal) throws IOException {
        Long userId = parseUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        PetResponseDto result = petService.uploadAndAttachImage(petId, userId, file);
        return ResponseEntity.ok(result);
    }

    /** Principal 파싱 유틸 */
    private Long parseUserId(Principal principal) {
        if (principal == null) return null;
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("잘못된 Principal.name 형식: {}", principal.getName());
            return null;
        }
    }
}
