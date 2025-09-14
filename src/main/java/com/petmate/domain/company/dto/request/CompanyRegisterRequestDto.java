package com.petmate.domain.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRegisterRequestDto {

    @NotBlank
    private String type; // "PERSONAL" or "BUSINESS"

    // 개인(일반인) 정보
    private String ssnFirst;
    private String ssnSecond;
    private String personalName;
    private String personalCompanyName;

    // 사업자 정보
    private String bizRegNo;
    private String corporationName;
    private String representativeName;

    // 주소 정보
    @NotBlank
    private String roadAddr;
    private String detailAddr;
    private String postcode;
    private String latitude;
    private String longitude;

    // 연락처 및 서비스
    @NotBlank
    private String tel;

    @NotBlank
    private String repService;

    @NotBlank
    private String services; // json 문자열

    @NotBlank
    private String operatingHours; // json 문자열

    private String introduction;

    // 파일 업로드
    private List<MultipartFile> images;
}
