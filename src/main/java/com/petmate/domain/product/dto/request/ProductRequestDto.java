package com.petmate.domain.product.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotNull(message = "업체 ID는 필수입니다.")
    private String companyId;

    @NotBlank(message = "서비스 타입은 필수입니다.")
    private String serviceType;

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotBlank(message = "가격은 필수입니다.")
    private Integer price;


    private Integer allDay;

    private Integer durationMin;

    @Size(max = 10000)
    private String introText;

    @NotBlank(message = "최소 반려동물 수는 필수입니다")
    private Integer minPet;

    @NotBlank(message = "최대 반려동물 수는 필수입니다.")
    private Integer maxPet;

}
