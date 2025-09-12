package com.petmate.domain.product.dto.response;

import com.petmate.domain.product.entity.ProductEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Integer id;
    private Integer companyId;
    private String companyName;
    private String serviceType;
    private String serviceTypeName;
    private String name;
    private Integer price;
    private Integer allDay;
    private Integer durationMin;
    private String introText;
    private Integer minPet;
    private Integer maxPet;
    private Integer isActive;
    private String isActiveName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponseDto from(ProductEntity entity) {
        return ProductResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .serviceType(entity.getServiceType())
                .serviceTypeName(getServiceTypeName(entity.getServiceType()))
                .name(entity.getName())
                .price(entity.getPrice())
                .allDay(entity.getAllDay())
                .durationMin(entity.getDurationMin())
                .introText(entity.getIntroText())
                .minPet(entity.getMinPet())
                .maxPet(entity.getMaxPet())
                .isActive(entity.getIsActive())
                .isActiveName(entity.getIsActive() == 1 ? "사용" : "중지")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String getServiceTypeName(String serviceType) {
        switch (serviceType) {
            case "P": return "펜션";
            case "H": return "호텔";
            case "C": return "카페";
            case "R": return "식당";
            default: return serviceType;
        }
    }
}
