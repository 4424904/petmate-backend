package com.petmate.domain.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressCreateRequestDto {

    @NotBlank(message = "주소 유형은 필수입니다")
    @Pattern(regexp = "^(집|회사|기타)$", message="유효하지 않은 주소 유형입니다.")
    private String type;

    @NotBlank(message = "도로명 주소는 필수입니다")
    private String address; // 도로명 주소

    private String detail; // 상세 주소

    private String alias; // 별칭(우리집, 친구네 등)

    private Boolean isDefault = false; // 기본 주소 여부

    private String postcode; // 우편번호

    private BigDecimal latitude; // 위도(x)

    private BigDecimal longitude; // 경도(y)

    private String ownerId; // 사용자 아이디
    private String label; // 주소 라벨("1", "2", "3")




}
