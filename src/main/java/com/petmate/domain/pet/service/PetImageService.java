package com.petmate.domain.pet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;
@Service
@RequiredArgsConstructor
public class PetImageService {

    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Transactional
    public String uploadPetMainImage(Integer petId, String contentType, byte[] bytes) {
        String ext = contentType != null && contentType.contains("png") ? "png"
                : contentType != null && contentType.contains("webp") ? "webp" : "jpg";

        String key = "pets/" + petId + "/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();

        s3.putObject(put, RequestBody.fromBytes(bytes));

        // ✅ 여기서 URL 만들지 말고 "키"만 반환
        return key;
    }
}
