// ====================================
// dto/response/PetResponseDto.java
// ====================================
package com.petmate.domain.pet.dto.response;

import com.petmate.domain.pet.entity.PetEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponseDto {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String imageUrl;
    private String species;
    private String speciesName;   // 한글명
    private Long breedId;
    private String breedName;     // 품종명
    private String gender;
    private String genderName;    // 한글명
    private BigDecimal ageYear;
    private BigDecimal weightKg;
    private Integer neutered;
    private String neuteredText;  // 한글명
    private String temper;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Entity -> DTO 변환 */
    public static PetResponseDto from(PetEntity entity) {
        return PetResponseDto.builder()
                .id(entity.getId())
                .ownerUserId(entity.getOwnerUserId())
                .name(entity.getName())
                .imageUrl(entity.getImageUrl())
                .species(entity.getSpecies())
                .speciesName(getSpeciesKoreanName(entity.getSpecies()))
                .breedId(entity.getBreedId())
                .breedName(entity.getBreed() != null ? entity.getBreed().getName() : null)
                .gender(entity.getGender())
                .genderName(getGenderKoreanName(entity.getGender()))
                .ageYear(entity.getAgeYear())
                .weightKg(entity.getWeightKg())
                .neutered(entity.getNeutered())
                .neuteredText(getNeuteredKoreanText(entity.getNeutered()))
                .temper(entity.getTemper())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** Entity와 품종명을 함께 받아서 DTO 생성 */
    public static PetResponseDto of(PetEntity entity, String breedName) {
        PetResponseDto dto = from(entity);
        dto.setBreedName(breedName);
        return dto;
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

    /** 성별 코드 -> 한글명 */
    private static String getGenderKoreanName(String gender) {
        if (gender == null) return "미입력";
        return "M".equalsIgnoreCase(gender) ? "남성" : "여성";
    }

    /** 중성화 여부 -> 한글 텍스트 */
    private static String getNeuteredKoreanText(Integer neutered) {
        if (neutered == null) return "미입력";
        return neutered == 1 ? "완료" : "미완료";
    }

    /** 나이 텍스트 */
    public String getAgeText() {
        if (ageYear == null || ageYear.compareTo(BigDecimal.ZERO) == 0) {
            return "미입력";
        }
        int years = ageYear.intValue();
        int months = (int) Math.round((ageYear.doubleValue() - years) * 12);
        return months > 0 ? String.format("%d살 %d개월", years, months) : String.format("%d살", years);
    }

    /** 체중 텍스트 */
    public String getWeightText() {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) == 0) {
            return "미입력";
        }
        return String.format("%.1fkg", weightKg);
    }
}
