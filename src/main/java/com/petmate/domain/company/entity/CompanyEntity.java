package com.petmate.domain.company.entity;

import com.petmate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "COMPANY")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CompanyEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TYPE", nullable = false, length = 1)
    private String type; // 개인,개인/법인사업자(company_type)

    @Column(name = "NAME", length = 200)
    private String name;    // 상호명

    @Column(name = "BIZ_REG_NO", length = 20, unique = true)
    private String bizRegNo;    // 사업자번호

    @Column(name = "REP_NAME", length = 80)
    private String repName;     // 대표자

    // 개인(일반인) 정보 추가
    @Column(name = "SSN_FIRST", length = 6)
    private String ssnFirst;    // 주민번호 앞자리 (생년월일)

    @Column(name = "TEL", length = 20)
    private String tel;     // 연락처

    @Column(name = "DESC_TEXT", columnDefinition = "TEXT")
    private String descText;   // 소개글

    @Column(name = "REP_SERVICE", length = 1)
    private String repService;  // 대표서비스(service_type)

    @Column(name = "SERVICES", columnDefinition = "JSON")
    private String services;    // 제공 서비스

    @Column(name = "OPERATING_HOURS", columnDefinition = "JSON")
    private String operatingHours;  // 운영시간 정보

    @Builder.Default
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status = "P"; // 승인상태(company_status) - 기본값 "pending"

    @Column(name = "ROAD_ADDR", length = 255, nullable = false)
    private String roadAddr;    // 도로명

    @Column(name = "DETAIL_ADDR", length = 255)
    private String detailAddr;  // 상세

    @Column(name = "POSTCODE", length = 10)
    private String postcode;    // 우편번호

    @Column(name = "LATITUDE", precision = 10, scale = 7)
    private BigDecimal latitude;     // 위도

    @Column(name = "LONGITUDE", precision = 10, scale = 7)
    private BigDecimal longitude;     // 경도

    @Column(name = "CREATED_BY", nullable = false)
    private Integer createdBy;  // 등록자(FK -> users.id)
    
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 등록일(기본값: current_timestamp)

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;    // 수정일(자동 업데이트)

}
