package com.petmate.domain.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 업체 이미지 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyImageDto {

    private Long id;                    // 이미지 ID
    private String filePath;            // 파일 경로
    private String originalName;        // 원본 파일명
    private String altText;             // 대체 텍스트
    private String description;         // 이미지 설명
    private Integer displayOrder;       // 표시 순서
    private Boolean isThumbnail;        // 썸네일 여부
    private String mimeType;            // MIME 타입
    private Long fileSize;              // 파일 크기
}