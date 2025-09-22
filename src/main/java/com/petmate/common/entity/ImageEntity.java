package com.petmate.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 범용 이미지 업로드 엔티티
 * 사용자, 펫, 업체, 자격증 등 모든 이미지를 관리
 */
@Entity
@Table(name = "IMAGE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id; // 이미지 ID (Primary Key)

    @Column(name = "REFERENCE_TYPE", nullable = false, length = 2)
    @Comment("참조 타입 코드 (01:사용자프로필, 02:펫프로필, 03:업체등록, 04:업체프로필, 05:펫메이트자격증 등)")
    private String referenceType; // 참조 타입 코드

    @Column(name = "REFERENCE_ID", nullable = false)
    @Comment("참조 대상 ID")
    private String referenceId; // 참조 대상의 ID (사용자 ID, 펫 ID, 업체 ID 등)

    @Column(name = "ORIGINAL_NAME", nullable = false, length = 255)
    @Comment("원본 파일명")
    private String originalName; // 원본 파일명

    @Column(name = "STORED_NAME", nullable = false, length = 255)
    @Comment("저장된 파일명 (UUID 등으로 변환)")
    private String storedName; // 저장된 파일명

    @Column(name = "FILE_PATH", nullable = false, length = 500)
    @Comment("파일 저장 경로")
    private String filePath; // 파일 저장 경로

    @Column(name = "FILE_SIZE", nullable = false)
    @Comment("파일 크기 (bytes)")
    private Long fileSize; // 파일 크기

    @Column(name = "FILE_EXTENSION", nullable = false, length = 10)
    @Comment("파일 확장자")
    private String fileExtension; // 파일 확장자

    @Column(name = "MIME_TYPE", nullable = false, length = 100)
    @Comment("MIME 타입")
    private String mimeType; // MIME 타입

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("표시 순서 (같은 참조 대상 내에서)")
    private Integer displayOrder; // 표시 순서

    @Column(name = "IS_THUMBNAIL", nullable = false, length = 1)
    @Comment("썸네일 여부 (Y/N)")
    private String isThumbnail; // 썸네일 여부

    @Column(name = "STATUS", nullable = false, length = 1)
    @Comment("상태 (A:활성, D:삭제)")
    private String status; // 상태 (A:활성, D:삭제)

    @Column(name = "ALT_TEXT", length = 255)
    @Comment("대체 텍스트 (접근성)")
    private String altText; // 대체 텍스트

    @Column(name = "DESCRIPTION", length = 500)
    @Comment("이미지 설명")
    private String description; // 이미지 설명

    // created_at, updated_at은 BaseEntity에서 자동 관리됩니다!
}