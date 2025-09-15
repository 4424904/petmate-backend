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
    public ImageEntity uploadSingleImage(MultipartFile file, String imageTypeCode, Long referenceId, boolean isThumbnail) throws IOException {
        return uploadSingleImage(file, imageTypeCode, referenceId, isThumbnail, null, null);
    }

    /**
     * 단일 이미지 업로드 (설명 포함)
     */
    public ImageEntity uploadSingleImage(MultipartFile file, String imageTypeCode, Long referenceId, 
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
    public List<ImageEntity> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode, Long referenceId) throws IOException {
        return uploadMultipleImages(files, imageTypeCode, referenceId, false);
    }

    /**
     * 다중 이미지 업로드 (첫 번째 이미지를 썸네일로 설정 옵션)
     */
    public List<ImageEntity> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode, Long referenceId,
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
    public List<ImageEntity> replaceAllImages(List<MultipartFile> files, String imageTypeCode, Long referenceId,
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
    public List<ImageEntity> getImagesByReference(String imageTypeCode, Long referenceId) {
        return imageRepository.findActiveImagesByReference(imageTypeCode, referenceId);
    }

    /**
     * 썸네일 이미지 조회
     */
    @Transactional(readOnly = true)
    public Optional<ImageEntity> getThumbnailByReference(String imageTypeCode, Long referenceId) {
        return imageRepository.findThumbnailByReference(imageTypeCode, referenceId);
    }

    /**
     * 첫 번째 이미지 조회 (썸네일이 없을 때 대체용)
     */
    @Transactional(readOnly = true)
    public Optional<ImageEntity> getFirstImageByReference(String imageTypeCode, Long referenceId) {
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
    public void deleteAllImagesByReference(String imageTypeCode, Long referenceId) {
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
    public void setThumbnail(Long imageId, String imageTypeCode, Long referenceId) {
        // 기존 썸네일 해제
        imageRepository.clearAllThumbnails(imageTypeCode, referenceId);
        // 새로운 썸네일 설정
        imageRepository.updateThumbnailStatus(imageId, "Y");
    }

    /**
     * 이미지 개수 조회
     */
    @Transactional(readOnly = true)
    public long getImageCount(String imageTypeCode, Long referenceId) {
        return imageRepository.countActiveImagesByReference(imageTypeCode, referenceId);
    }

    /**
     * 여러 참조 ID들의 썸네일 이미지들 조회 (배치 조회용)
     */
    @Transactional(readOnly = true)
    public List<ImageEntity> getThumbnailsByReferenceIds(String imageTypeCode, List<Long> referenceIds) {
        return imageRepository.findThumbnailsByReferenceIds(imageTypeCode, referenceIds);
    }

    // Private helper methods
    
    /**
     * 기본 파라미터 검증
     */
    private void validateBasicParams(String imageTypeCode, Long referenceId) {
        if (imageTypeCode == null || imageTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 타입 코드는 필수입니다.");
        }
        if (referenceId == null || referenceId <= 0) {
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