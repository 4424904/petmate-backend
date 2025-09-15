package com.petmate.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.petmate.common.util.CodeUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${app.upload.dir:C:/petmate}")
    private String uploadRootDir;

    private static final String IMAGES_SUBDIR = "images";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final CodeUtil codeUtil;
    
    public FileUtil(CodeUtil codeUtil) {
        this.codeUtil = codeUtil;
    }
    
    public String uploadSingleImage(MultipartFile file) throws IOException {
        return uploadSingleImage(file, null);
    }
    
    public String uploadSingleImage(MultipartFile file, String imageTypeCode) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        validateImageFile(file);
        
        String uploadPath = createUploadDirectory(imageTypeCode);
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String filePath = uploadPath + fileName;
        
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());
        
        String relativePath = getRelativePath(imageTypeCode) + fileName;
        return relativePath;
    }
    
    public List<String> uploadMultipleImages(List<MultipartFile> files) throws IOException {
        return uploadMultipleImages(files, null);
    }
    
    public List<String> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        
        List<String> uploadedFiles = new ArrayList<>();
        String uploadPath = createUploadDirectory(imageTypeCode);
        String relativePath = getRelativePath(imageTypeCode);
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                validateImageFile(file);
                
                String fileName = generateUniqueFileName(file.getOriginalFilename());
                String filePath = uploadPath + fileName;
                
                Path path = Paths.get(filePath);
                Files.write(path, file.getBytes());
                
                uploadedFiles.add(relativePath + fileName);
            }
        }
        
        if (uploadedFiles.isEmpty()) {
            throw new IllegalArgumentException("유효한 파일이 없습니다.");
        }
        
        return uploadedFiles;
    }
    
    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 10MB를 초과합니다.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS) + ")");
        }
    }
    
    private String createUploadDirectory(String imageTypeCode) throws IOException {
        String folderName = "";
        if (imageTypeCode != null) {
            folderName = getFolderNameByImageType(imageTypeCode) + File.separator;
        }

        String uploadPath = uploadRootDir + File.separator + IMAGES_SUBDIR + File.separator + folderName;
        Path path = Paths.get(uploadPath);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        return uploadPath;
    }
    
    /**
     * 이미지 타입 코드로부터 폴더명을 생성
     * CODE_NAME_ENG를 camelCase로 변환
     */
    private String getFolderNameByImageType(String imageTypeCode) {
        String codeNameEng = codeUtil.getCodeNameEng("IMAGE_TYPE", imageTypeCode);
        if (StringUtil.isEmpty(codeNameEng)) {
            return "misc"; // 기타 폴더
        }
        return StringUtil.toCamelCase(codeNameEng);
    }
    
    /**
     * 상대 경로 반환 (웹에서 접근할 수 있는 경로)
     */
    private String getRelativePath(String imageTypeCode) {
        if (imageTypeCode == null) {
            return IMAGES_SUBDIR + "/";
        }
        String folderName = getFolderNameByImageType(imageTypeCode);
        return IMAGES_SUBDIR + "/" + folderName + "/";
    }
    
    private String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return timestamp + "_" + uuid + "." + extension;
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    public boolean deleteFile(String filePath) {
        try {
            String fullPath = System.getProperty("user.dir") + File.separator + filePath;
            Path path = Paths.get(fullPath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}
