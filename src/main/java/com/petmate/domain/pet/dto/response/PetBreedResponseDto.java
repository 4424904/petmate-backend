package com.petmate.domain.pet.dto.response;

import com.petmate.domain.pet.entity.PetBreedEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetBreedResponseDto {

    private Long id;
    private String species;
    private String speciesName; // 한글명
    private String korName;     // DB name 컬럼 매핑

    /** Entity -> DTO 변환 */
    public static PetBreedResponseDto from(PetBreedEntity entity) {
        return PetBreedResponseDto.builder()
                .id(entity.getId())
                .species(entity.getSpecies())
                .speciesName(getSpeciesKoreanName(entity.getSpecies()))
                .korName(entity.getName())
                .build();
    }

    /** 동물 종류 코드 -> 한글명 */
    private static String getSpeciesKoreanName(String species) {
        if (species == null) return "미입력";
        return switch (species) {
            case "D" -> "강아지";
            case "C" -> "고양이";
            case "R" -> "토끼";
            case "S" -> "설치류";
            case "H" -> "말";
            case "B" -> "새";
            case "P" -> "파충류";
            case "F" -> "가축동물";
            case "O" -> "기타";
            default -> "미입력";
        };
    }
}
