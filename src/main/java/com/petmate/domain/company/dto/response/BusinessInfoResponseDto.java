package com.petmate.domain.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessInfoResponseDto {

    private String businessNumber;     // 사업자등록번호
    private String companyName;        // 상호명 (b_nm)
    private String representativeName; // 대표자명 (p_nm)
    private String startDate;          // 개업일자 (start_dt)
    private String businessSector;     // 업종 (b_sector)
    private String businessType;       // 사업자구분 (b_type)
    private String address;            // 주소 (b_adr)
    private String status;             // 사업자 상태 (b_stt)
    private boolean isValid;           // 진위확인 결과
    private String message;            // 결과 메시지

}