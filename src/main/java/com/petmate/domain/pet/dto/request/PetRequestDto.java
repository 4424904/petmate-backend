// ====================================
// dto/request/PetRequestDto.java
// ====================================
package com.petmate.domain.pet.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Spring Boot 3.x 사용 시
import jakarta.validation.constraints.*;
// Spring Boot 2.x 사용 시는 아래 주석 해제
// import javax.validation.constraints.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetRequestDto {

    @NotBlank(message = "반려동물 이름은 필수입니다.")
    @Size(max = 50, message = "반려동물 이름은 50자 이내로 입력해주세요.")
    private String name;

    @Size(max = 500, message = "이미지 URL은 500자 이내로 입력해주세요.")
    private String imageUrl;

    @NotBlank(message = "동물 종류는 필수입니다.")
    @Pattern(regexp = "^[DCRSHBPFO]$", message = "유효하지 않은 동물 종류입니다. (D:강아지, C:고양이, R:토끼, S:설치류, H:말, B:새, P:파충류, F:가축, O:기타)")
    private String species;

    private Long breedId;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "^[MF]$", message = "성별은 M(남성) 또는 F(여성)이어야 합니다.")
    private String gender;

    @DecimalMin(value = "0.0", message = "나이는 0 이상이어야 합니다.")
    @DecimalMax(value = "50.0", message = "나이는 50 이하로 입력해주세요.")
    private BigDecimal ageYear;

    @DecimalMin(value = "0.0", message = "체중은 0 이상이어야 합니다.")
    @DecimalMax(value = "999.99", message = "체중은 999.99kg 이하로 입력해주세요.")
    private BigDecimal weightKg;

    @Min(value = 0, message = "중성화 여부는 0(미완료) 또는 1(완료)이어야 합니다.")
    @Max(value = 1, message = "중성화 여부는 0(미완료) 또는 1(완료)이어야 합니다.")
    private Integer neutered = 0; // 기본값: 미완료

    @Size(max = 50, message = "성격은 50자 이내로 입력해주세요.")
    private String temper;

    @Size(max = 1000, message = "특이사항은 1000자 이내로 입력해주세요.")
    private String note;

    /**
     * Entity로 변환하는 메서드
     */
    public com.petmate.domain.pet.entity.PetEntity toEntity(Long ownerUserId) {
        return com.petmate.domain.pet.entity.PetEntity.builder()
                .ownerUserId(ownerUserId)
                .name(this.name)
                .imageUrl(this.imageUrl)
                .species(this.species)
                .breedId(this.breedId)
                .gender(this.gender)
                .ageYear(this.ageYear)
                .weightKg(this.weightKg)
                .neutered(this.neutered)
                .temper(this.temper)
                .note(this.note)
                .build();
    }
}