// com/petmate/common/storage/LocalStorageService.java
package com.petmate.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:C:/petmate}")
    private String uploadRoot;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public String save(MultipartFile file, String subDir, String hint) {
        try {
            String clean = file.getOriginalFilename()==null? "file"
                    : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
            String fname = TS.format(LocalDateTime.now()) + "_" + hint + "_" + clean;
            Path dir = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(subDir).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fname);
            file.transferTo(target);
            return subDir.replace('\\','/') + "/" + fname;
        } catch (Exception e) {
            throw new RuntimeException("local store failed", e);
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            Path p = Paths.get(uploadRoot).resolve(storedPath).normalize();
            Files.deleteIfExists(p);
        } catch (Exception ignore) {}
    }

    @Override
    public String saveBytes(byte[] bytes, String subDir, String hint, String ext) {
        try {
            String fname = TS.format(LocalDateTime.now()) + "_" + hint + "." + (ext==null? "bin": ext);
            Path dir = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(subDir).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fname);
            Files.write(target, bytes);
            return subDir.replace('\\','/') + "/" + fname;
        } catch (Exception e) {
            throw new RuntimeException("local store failed (bytes)", e);
        }
    }
}
