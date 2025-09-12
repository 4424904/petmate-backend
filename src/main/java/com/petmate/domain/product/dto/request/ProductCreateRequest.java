package com.petmate.domain.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotNull
    private Integer companyId;

    @NotBlank
    @Size(max = 1)
    private String serviceType;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull // @NotBlank는 문자열용, 숫자는 @NotNull 사용
    @Min(0)
    private Integer price;

    private Integer allDay = 0;

    private Integer durationMin;

    private String introText;

    @NotNull
    @Min(1)
    private Integer minPet = 1;

    @NotNull
    @Min(1)
    private Integer maxPet = 1; // 0이 아닌 1로 변경

    private Integer isActive = 1;
}
