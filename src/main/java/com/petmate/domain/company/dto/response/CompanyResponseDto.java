package com.petmate.domain.company.dto.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDto {


    private Integer id;
    private String type; // 개인/법인(company_type)
    private String name;    // 상호명
    private String bizRegNo;    // 사업자번호
    private String repName;     // 대표자
    
    // 개인(일반인) 정보 추가
    private String ssnFirst;    // 주민번호 앞자리 (생년월일)
    
    private String tel;     // 연락처
    private String descText;   // 소개글
    private String repService;  // 대표서비스(service_type)
    private String services;    // 제공 서비스
    private String operatingHours;  // 운영시간 정보
    private String status; // 승인상태(company_status)
    private String roadAddr;    // 도로명
    private String detailAddr;  // 상세
    private String postcode;    // 우편번호
    private BigDecimal lat;     // 위도
    private BigDecimal lng;     // 경도
    private Integer createdBy;  // 등록자(FK -> users.id)
    private LocalDateTime createdAt;    // 등록일(기본값: current_timestamp)

}
