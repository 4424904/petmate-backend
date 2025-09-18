package com.petmate.common.service;

import com.petmate.common.entity.ImageEntity;
import com.petmate.common.repository.ImageRepository;
import com.petmate.common.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 이미지 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {

    private final ImageRepository imageRepository;
    private final FileUtil fileUtil;

    /**
     * 단일 이미지 업로드
     */
    public ImageEntity uploadSingleImage(MultipartFile file, String imageTypeCode, String referenceId, boolean isThumbnail) throws IOException {
        return uploadSingleImage(file, imageTypeCode, referenceId, isThumbnail, null, null);
    }

    /**
     * 단일 이미지 업로드 (설명 포함)
     */
    public ImageEntity uploadSingleImage(MultipartFile file, String imageTypeCode, String referenceId, 
                                       boolean isThumbnail, String altText, String description) throws IOException {
        
        validateBasicParams(imageTypeCode, referenceId);
        
        // 파일 업로드
        String uploadedFilePath = fileUtil.uploadSingleImage(file, imageTypeCode);
        
        // 표시 순서 계산
        Integer maxOrder = imageRepository.findMaxDisplayOrderByReference(imageTypeCode, referenceId);
        Integer nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;
        
        // 썸네일 설정 시 기존 썸네일 해제
        if (isThumbnail) {
            imageRepository.clearAllThumbnails(imageTypeCode, referenceId);
        }
        
        // 이미지 엔티티 생성
        ImageEntity imageEntity = ImageEntity.builder()
                .referenceType(imageTypeCode)
                .referenceId(referenceId)
                .originalName(file.getOriginalFilename())
                .storedName(extractStoredName(uploadedFilePath))
                .filePath(uploadedFilePath)
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .mimeType(file.getContentType())
                .displayOrder(nextOrder)
                .isThumbnail(isThumbnail ? "Y" : "N")
                .status("A")
                .altText(altText)
                .description(description)
                .build();
        
        return imageRepository.save(imageEntity);
    }

    /**
     * 다중 이미지 업로드
     */
    public List<ImageEntity> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode, String referenceId) throws IOException {
        return uploadMultipleImages(files, imageTypeCode, referenceId, false);
    }

    /**
     * 다중 이미지 업로드 (첫 번째 이미지를 썸네일로 설정 옵션)
     */
    public List<ImageEntity> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode, String referenceId,
                                                boolean setFirstAsThumbnail) throws IOException {

        validateBasicParams(imageTypeCode, referenceId);

        // 파일 업로드
        List<String> uploadedFilePaths = fileUtil.uploadMultipleImages(files, imageTypeCode);

        // 현재 최대 표시 순서 조회
        Integer maxOrder = imageRepository.findMaxDisplayOrderByReference(imageTypeCode, referenceId);
        Integer startOrder = (maxOrder != null) ? maxOrder + 1 : 1;

        // 첫 번째 이미지를 썸네일로 설정하는 경우 기존 썸네일 해제
        if (setFirstAsThumbnail) {
            imageRepository.clearAllThumbnails(imageTypeCode, referenceId);
        }

        List<ImageEntity> savedImages = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String uploadedPath = uploadedFilePaths.get(i);
            boolean isThumbnail = setFirstAsThumbnail && i == 0;

            ImageEntity imageEntity = ImageEntity.builder()
                    .referenceType(imageTypeCode)
                    .referenceId(referenceId)
                    .originalName(file.getOriginalFilename())
                    .storedName(extractStoredName(uploadedPath))
                    .filePath(uploadedPath)
                    .fileSize(file.getSize())
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .mimeType(file.getContentType())
                    .displayOrder(startOrder + i)
                    .isThumbnail(isThumbnail ? "Y" : "N")
                    .status("A")
                    .build();

            savedImages.add(imageRepository.save(imageEntity));
        }

        return savedImages;
    }

    /**
     * 기존 이미지들을 모두 삭제하고 새로운 이미지들로 교체
     */
    public List<ImageEntity> replaceAllImages(List<MultipartFile> files, String imageTypeCode, String referenceId,
                                            boolean setFirstAsThumbnail) throws IOException {

        validateBasicParams(imageTypeCode, referenceId);

        // 1. 기존 이미지들 모두 삭제
        deleteAllImagesByReference(imageTypeCode, referenceId);

        // 2. 새로운 이미지들 업로드
        List<String> uploadedFilePaths = fileUtil.uploadMultipleImages(files, imageTypeCode);

        List<ImageEntity> savedImages = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String uploadedPath = uploadedFilePaths.get(i);
            boolean isThumbnail = setFirstAsThumbnail && i == 0;

            ImageEntity imageEntity = ImageEntity.builder()
                    .referenceType(imageTypeCode)
                    .referenceId(referenceId)
                    .originalName(file.getOriginalFilename())
                    .storedName(extractStoredName(uploadedPath))
                    .filePath(uploadedPath)
                    .fileSize(file.getSize())
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .mimeType(file.getContentType())
                    .displayOrder(i + 1)  // 1부터 시작
                    .isThumbnail(isThumbnail ? "Y" : "N")
                    .status("A")
                    .build();

            savedImages.add(imageRepository.save(imageEntity));
        }

        return savedImages;
    }

    /**
     * 참조 대상의 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ImageEntity> getImagesByReference(String imageTypeCode, String referenceId) {
        return imageRepository.findActiveImagesByReference(imageTypeCode, referenceId);
    }

    /**
     * 썸네일 이미지 조회
     */
    @Transactional(readOnly = true)
    public Optional<ImageEntity> getThumbnailByReference(String imageTypeCode, String referenceId) {
        return imageRepository.findThumbnailByReference(imageTypeCode, referenceId);
    }

    /**
     * 첫 번째 이미지 조회 (썸네일이 없을 때 대체용)
     */
    @Transactional(readOnly = true)
    public Optional<ImageEntity> getFirstImageByReference(String imageTypeCode, String referenceId) {
        return imageRepository.findFirstImageByReference(imageTypeCode, referenceId);
    }

    /**
     * 이미지 삭제 (소프트 삭제)
     */
    public void deleteImage(Long imageId) {
        Optional<ImageEntity> imageOpt = imageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            ImageEntity image = imageOpt.get();
            // 파일 시스템에서 실제 파일 삭제
            fileUtil.deleteFile(image.getFilePath());
            // DB에서 소프트 삭제
            imageRepository.softDeleteById(imageId);
        }
    }

    /**
     * 참조 대상의 모든 이미지 삭제
     */
    public void deleteAllImagesByReference(String imageTypeCode, String referenceId) {
        List<ImageEntity> images = getImagesByReference(imageTypeCode, referenceId);
        
        // 파일 시스템에서 실제 파일들 삭제
        for (ImageEntity image : images) {
            fileUtil.deleteFile(image.getFilePath());
        }
        
        // DB에서 소프트 삭제
        imageRepository.softDeleteAllByReference(imageTypeCode, referenceId);
    }

    /**
     * 썸네일 설정
     */
    public void setThumbnail(Long imageId, String imageTypeCode, String referenceId) {
        // 기존 썸네일 해제
        imageRepository.clearAllThumbnails(imageTypeCode, referenceId);
        // 새로운 썸네일 설정
        imageRepository.updateThumbnailStatus(imageId, "Y");
    }

    /**
     * 이미지 개수 조회
     */
    @Transactional(readOnly = true)
    public long getImageCount(String imageTypeCode, String referenceId) {
        return imageRepository.countActiveImagesByReference(imageTypeCode, referenceId);
    }

    /**
     * 여러 참조 ID들의 썸네일 이미지들 조회 (배치 조회용)
     */
    @Transactional(readOnly = true)
    public List<ImageEntity> getThumbnailsByReferenceIds(String imageTypeCode, List<String> referenceIds) {
        return imageRepository.findThumbnailsByReferenceIds(imageTypeCode, referenceIds);
    }

    /**
     * URL에서 이미지 다운로드하여 저장
     */
    public ImageEntity uploadImageFromUrl(String imageUrl, String imageTypeCode, String referenceId,
                                        boolean isThumbnail, String altText, String description) throws IOException {
        validateBasicParams(imageTypeCode, referenceId);

        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.startsWith("http")) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imageUrl);
        }

        try {
            log.info("URL에서 이미지 다운로드 시작: {}", imageUrl);

            // URL에서 이미지 다운로드
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                // FileUtil을 사용해서 InputStream을 파일로 저장
                String uploadedFilePath = fileUtil.uploadImageFromInputStream(inputStream, imageTypeCode, getFileExtensionFromUrl(imageUrl));

                // 표시 순서 계산
                Integer maxOrder = imageRepository.findMaxDisplayOrderByReference(imageTypeCode, referenceId);
                Integer nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;

                // ImageEntity 생성 및 저장
                ImageEntity imageEntity = ImageEntity.builder()
                        .referenceType(imageTypeCode)
                        .referenceId(referenceId)
                        .filePath(uploadedFilePath)
                        .storedName(extractStoredName(uploadedFilePath))
                        .originalName(extractFileNameFromUrl(imageUrl))
                        .fileSize(0L) // URL 다운로드이므로 크기는 0으로 설정
                        .fileExtension(getFileExtensionFromUrl(imageUrl))
                        .mimeType("image/" + getFileExtensionFromUrl(imageUrl))
                        .altText(altText)
                        .description(description)
                        .displayOrder(nextOrder)
                        .isThumbnail(isThumbnail ? "Y" : "N")
                        .status("A")
                        .build();

                ImageEntity savedImage = imageRepository.save(imageEntity);
                log.info("URL 이미지 저장 완료: {} -> {}", imageUrl, uploadedFilePath);

                return savedImage;
            }
        } catch (Exception e) {
            log.error("URL 이미지 다운로드 실패: {}", imageUrl, e);
            throw new IOException("URL에서 이미지 다운로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 프로필 이미지 조회 또는 소셜 이미지 자동 저장
     * (ImageType, ReferenceId, Status=A) 조회 시 없으면 소셜 프로필 자동 저장
     */
    public Optional<ImageEntity> getOrCreateSocialProfileImage(String imageTypeCode, String referenceId, String socialImageUrl) {
        // 기존 이미지 조회
        List<ImageEntity> existingImages = getImagesByReference(imageTypeCode, referenceId);

        if (!existingImages.isEmpty()) {
            // 기존 이미지가 있으면 첫 번째(또는 썸네일) 반환
            return existingImages.stream()
                    .filter(img -> "Y".equals(img.getIsThumbnail()))
                    .findFirst()
                    .or(() -> existingImages.stream().findFirst());
        }

        // 기존 이미지가 없고 소셜 이미지 URL이 있으면 자동 저장
        if (socialImageUrl != null && socialImageUrl.startsWith("http")) {
            try {
                log.info("소셜 프로필 이미지 자동 저장: imageType={}, referenceId={}, url={}",
                         imageTypeCode, referenceId, socialImageUrl);

                ImageEntity socialImage = uploadImageFromUrl(
                    socialImageUrl,
                    imageTypeCode,
                    referenceId,
                    true, // 썸네일로 설정
                    "소셜 프로필 이미지",
                    "소셜 로그인에서 가져온 기본 프로필 이미지"
                );

                return Optional.of(socialImage);
            } catch (IOException e) {
                log.error("소셜 프로필 이미지 자동 저장 실패: {}", socialImageUrl, e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * URL에서 파일 확장자 추출
     */
    private String getFileExtensionFromUrl(String url) {
        if (url == null || url.isBlank()) return "jpg";

        // 구글/소셜 이미지 URL 패턴을 먼저 체크 (우선순위)
        if (url.contains("googleusercontent.com") ||
            url.contains("kakaocdn.net") ||
            url.contains("phinf.pstatic.net")) {
            return "jpg"; // 소셜 이미지는 보통 jpg
        }

        // 쿼리 파라미터 제거
        String cleanUrl = url.split("\\?")[0];

        // 확장자 추출
        int lastDotIndex = cleanUrl.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < cleanUrl.length() - 1) {
            String extension = cleanUrl.substring(lastDotIndex + 1).toLowerCase();
            // 확장자가 빈 문자열이 아니고 유효한 확장자인지 검증
            if (!extension.isBlank() && extension.matches("^[a-z]{3,4}$")) {
                return extension;
            }
        }

        return "jpg"; // 기본값
    }

    /**
     * URL에서 파일명 추출
     */
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isBlank()) return "social_profile.jpg";

        try {
            String cleanUrl = url.split("\\?")[0];
            String fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
            return fileName.isBlank() ? "social_profile.jpg" : fileName;
        } catch (Exception e) {
            return "social_profile.jpg";
        }
    }

    // Private helper methods
    
    /**
     * 기본 파라미터 검증
     */
        private void validateBasicParams(String imageTypeCode, String referenceId) {
        if (imageTypeCode == null || imageTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 타입 코드는 필수입니다.");
        }
        if (referenceId == null || referenceId.trim().isEmpty()) {
            throw new IllegalArgumentException("참조 ID는 필수이며 0보다 커야 합니다.");
        }
    }
    
    private String extractStoredName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}