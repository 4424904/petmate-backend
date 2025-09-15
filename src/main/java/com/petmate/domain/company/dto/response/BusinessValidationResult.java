package com.petmate.domain.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessValidationResult {
    
    private String businessNumber;      // 사업자등록번호
    private boolean isValid;           // 유효성 여부
    private String message;            // 결과 메시지
    private String companyName;        // 회사명 (필요시 사용)
    private String status;             // 상태 (01: 유효, 02: 무효)
}
