package com.petmate.domain.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "반려인 ID는 필수입니다")
    private Integer ownerUserId;

    @NotNull(message = "업체 ID는 필수입니다")
    private Integer companyId;

    @NotNull(message = "상품 ID는 필수입니다")
    private Integer productId;

    @NotNull(message = "시작일시는 필수입니다")
//    @Future(message = "시작일시는 현재보다 미래여야 합니다")
    private LocalDateTime startDt;

    @NotNull(message = "종료일시는 필수입니다")
    private LocalDateTime endDt;

    @Min(value = 1, message = "펫 마리수는 1 이상이어야 합니다")
    @Max(value = 10, message = "펫 마리수는 10 이하여야 합니다")
    private Integer petCount;

    // 프론트엔드에서 받는 반려동물 ID 목록 (배열)
    @JsonProperty("selectedPetIds")
    private List<Long> selectedPetIdsList;

    // 데이터베이스에 저장될 JSON 문자열 (Jackson에서 제외)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String selectedPetIds;

    @Size(max = 500, message = "특이사항은 500자 이하여야 합니다")
    private String specialRequest;

    @NotNull(message = "총 금액은 필수입니다")
    @Min(value = 0, message = "총 금액은 0 이상이어야 합니다")
    private Integer totalPrice;

    // MyBatis insert 후 생성된 ID를 받기 위한 필드
    private Integer id;


}
