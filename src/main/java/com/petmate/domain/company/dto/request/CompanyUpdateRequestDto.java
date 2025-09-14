package com.petmate.domain.company.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUpdateRequestDto {

    private String name;            // 상호명
    private String tel;             // 연락처
    private String descText;        // 소개글
    private String roadAddr;        // 도로명 주소
    private String detailAddr;      // 상세주소
    private String postcode;        // 우편번호
    private String latitude;        // 위도
    private String longitude;       // 경도
    private String repService;      // 대표서비스
    private String services;        // 제공 서비스 (JSON)
    private String operatingHours;  // 운영시간 (JSON)
}
