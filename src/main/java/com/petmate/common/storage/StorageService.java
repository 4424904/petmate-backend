// com/petmate/common/storage/StorageService.java
package com.petmate.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String save(MultipartFile file, String subDir, String filenameHint);
    void delete(String storedPath);

    /** 외부 URL에서 받아온 바이트를 저장 */
    String saveBytes(byte[] bytes, String subDir, String filenameHint, String ext);
}
