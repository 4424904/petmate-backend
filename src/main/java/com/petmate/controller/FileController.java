package com.petmate.controller;

import com.petmate.common.entity.ImageEntity;
import com.petmate.common.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {
    
    private final ImageService imageService;
    
    @PostMapping("/upload/single")
    public ResponseEntity<Map<String, Object>> uploadSingleImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageTypeCode") String imageTypeCode,
            @RequestParam("referenceId") String referenceId,
            @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ImageEntity savedImage = imageService.uploadSingleImage(file, imageTypeCode, referenceId, isThumbnail, altText, description);
            response.put("success", true);
            response.put("message", "이미지 업로드 성공");
            response.put("imageId", savedImage.getId());
            response.put("filePath", savedImage.getFilePath());
            response.put("originalName", savedImage.getOriginalName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("imageTypeCode") String imageTypeCode,
            @RequestParam("referenceId") String referenceId,
            @RequestParam(value = "setFirstAsThumbnail", defaultValue = "false") boolean setFirstAsThumbnail) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<ImageEntity> savedImages = imageService.uploadMultipleImages(files, imageTypeCode, referenceId, setFirstAsThumbnail);
            response.put("success", true);
            response.put("message", "이미지 업로드 성공");
            response.put("images", savedImages.stream().map(image -> {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("imageId", image.getId());
                imageInfo.put("filePath", image.getFilePath());
                imageInfo.put("originalName", image.getOriginalName());
                imageInfo.put("isThumbnail", "Y".equals(image.getIsThumbnail()));
                return imageInfo;
            }).toList());
            response.put("uploadCount", savedImages.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/replace")
    public ResponseEntity<Map<String, Object>> replaceAllImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("imageTypeCode") String imageTypeCode,
            @RequestParam("referenceId") String referenceId,
            @RequestParam(value = "setFirstAsThumbnail", defaultValue = "false") boolean setFirstAsThumbnail) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<ImageEntity> savedImages = imageService.replaceAllImages(files, imageTypeCode, referenceId, setFirstAsThumbnail);
            response.put("success", true);
            response.put("message", "이미지 교체 성공");
            response.put("images", savedImages.stream().map(image -> {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("imageId", image.getId());
                imageInfo.put("filePath", image.getFilePath());
                imageInfo.put("originalName", image.getOriginalName());
                imageInfo.put("isThumbnail", "Y".equals(image.getIsThumbnail()));
                return imageInfo;
            }).toList());
            response.put("uploadCount", savedImages.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "이미지 교체 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteImage(@RequestParam("imageId") Long imageId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            imageService.deleteImage(imageId);
            response.put("success", true);
            response.put("message", "이미지 삭제 성공");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 삭제 실패: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getImageList(
            @RequestParam("imageTypeCode") String imageTypeCode,
            @RequestParam("referenceId") String referenceId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ImageEntity> images = imageService.getImagesByReference(imageTypeCode, referenceId);
            response.put("success", true);
            response.put("images", images.stream().map(image -> {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("imageId", image.getId());
                imageInfo.put("filePath", image.getFilePath());
                imageInfo.put("originalName", image.getOriginalName());
                imageInfo.put("isThumbnail", "Y".equals(image.getIsThumbnail()));
                imageInfo.put("displayOrder", image.getDisplayOrder());
                imageInfo.put("altText", image.getAltText());
                imageInfo.put("description", image.getDescription());
                return imageInfo;
            }).toList());
            response.put("count", images.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/thumbnail")
    public ResponseEntity<Map<String, Object>> setThumbnail(
            @RequestParam("imageId") Long imageId,
            @RequestParam("imageTypeCode") String imageTypeCode,
            @RequestParam("referenceId") String referenceId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            imageService.setThumbnail(imageId, imageTypeCode, referenceId);
            response.put("success", true);
            response.put("message", "썸네일 설정 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "썸네일 설정 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}