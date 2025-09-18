package com.petmate.common.util;

import com.petmate.common.util.CodeUtil;
import com.petmate.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;
    private final CodeUtil codeUtil;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadSingleImage(MultipartFile file) throws IOException {
        return uploadSingleImage(file, null);
    }

    public String uploadSingleImage(MultipartFile file, String imageTypeCode) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        validateImageFile(file);

        String fileName = generateS3FileName(file.getOriginalFilename(), imageTypeCode);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3 파일 업로드 성공: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", fileName, e);
            throw new IOException("S3 파일 업로드 실패: " + e.getMessage());
        }
    }

    public List<String> uploadMultipleImages(List<MultipartFile> files) throws IOException {
        return uploadMultipleImages(files, null);
    }

    public List<String> uploadMultipleImages(List<MultipartFile> files, String imageTypeCode) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        List<String> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                validateImageFile(file);
                String fileName = uploadSingleImage(file, imageTypeCode);
                uploadedFiles.add(fileName);
            }
        }

        if (uploadedFiles.isEmpty()) {
            throw new IllegalArgumentException("유효한 파일이 없습니다.");
        }

        return uploadedFiles;
    }

    public String uploadImageFromInputStream(InputStream inputStream, String imageTypeCode, String fileExtension) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream이 null입니다.");
        }

        if (fileExtension == null || fileExtension.isBlank()) {
            fileExtension = "jpg";
        }
        String extension = fileExtension.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS) + ")");
        }

        String fileName = generateS3FileNameWithExtension(extension, imageTypeCode);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("image/" + extension)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));

            log.info("S3 InputStream 파일 업로드 성공: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("S3 InputStream 파일 업로드 실패: {}", fileName, e);
            throw new IOException("S3 파일 업로드 실패: " + e.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        return getPresignedUrl(fileName, Duration.ofHours(24));
    }

    public String getPresignedUrl(String fileName, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.info("Pre-signed URL 생성 성공: {}", fileName);
            return url;

        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패: {}", fileName, e);
            // 실패 시 fallback으로 직접 URL 반환
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName,
                    s3Client.serviceClientConfiguration().region().id(),
                    fileName);
        }
    }

    public boolean deleteFile(String fileName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: {}", fileName);
            return true;

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileName, e);
            return false;
        }
    }

    public boolean doesFileExist(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 실패: {}", fileName, e);
            return false;
        }
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

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    public String generateS3FileName(String originalFilename, String imageTypeCode) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String imageTypeCamel = "";
        if (imageTypeCode != null) {
            imageTypeCamel = getFolderNameByImageType(imageTypeCode) + "_";
        }

        return timestamp + "_" + imageTypeCamel + uuid + "." + extension;
    }

    public String generateS3FileNameWithExtension(String extension, String imageTypeCode) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String imageTypeCamel = "";
        if (imageTypeCode != null) {
            imageTypeCamel = getFolderNameByImageType(imageTypeCode) + "_";
        }

        return timestamp + "_" + imageTypeCamel + uuid + "." + extension;
    }

    private String getFolderNameByImageType(String imageTypeCode) {
        String codeNameEng = codeUtil.getCodeNameEng("IMAGE_TYPE", imageTypeCode);
        if (StringUtil.isEmpty(codeNameEng)) {
            return "misc";
        }
        return StringUtil.toCamelCase(codeNameEng);
    }
}