package com.petmate.domain.address.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDto {

    private Integer id;
    private String type;
    private String address;
    private String detail;
    private String alias;
    private Boolean isDefault;
    private String postcode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
