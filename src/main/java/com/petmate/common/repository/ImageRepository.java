package com.petmate.common.repository;

import com.petmate.common.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 이미지 Repository
 */
@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    /**
     * 특정 참조 타입과 참조 ID로 활성 상태인 이미지 목록 조회 (표시 순서대로)
     */
    @Query("SELECT i FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.status = 'A' ORDER BY i.displayOrder ASC")
    List<ImageEntity> findActiveImagesByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 특정 참조 타입과 참조 ID로 썸네일 이미지 조회
     */
    @Query("SELECT i FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.isThumbnail = 'Y' AND i.status = 'A'")
    Optional<ImageEntity> findThumbnailByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 특정 참조 타입과 참조 ID로 첫 번째 이미지 조회 (썸네일이 없을 때 대체용)
     */
    @Query("SELECT i FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.status = 'A' ORDER BY i.displayOrder ASC")
    Optional<ImageEntity> findFirstImageByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 저장된 파일명으로 이미지 조회
     */
    Optional<ImageEntity> findByStoredNameAndStatus(String storedName, String status);

    /**
     * 특정 참조 타입과 참조 ID로 이미지 개수 조회 (활성 상태만)
     */
    @Query("SELECT COUNT(i) FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.status = 'A'")
    long countActiveImagesByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 특정 참조 타입과 참조 ID의 최대 표시 순서 조회
     */
    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.status = 'A'")
    Integer findMaxDisplayOrderByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 특정 참조 ID들의 썸네일 이미지들 조회 (배치 조회용)
     */
    @Query("SELECT i FROM ImageEntity i WHERE i.referenceType = :referenceType AND i.referenceId IN :referenceIds AND i.isThumbnail = 'Y' AND i.status = 'A'")
    List<ImageEntity> findThumbnailsByReferenceIds(@Param("referenceType") String referenceType, @Param("referenceIds") List<Long> referenceIds);

    /**
     * 삭제 상태로 변경 (실제 삭제가 아닌 소프트 삭제)
     */
    @Modifying
    @Query("UPDATE ImageEntity i SET i.status = 'D' WHERE i.id = :id")
    void softDeleteById(@Param("id") Long id);

    /**
     * 특정 참조의 모든 이미지를 삭제 상태로 변경
     */
    @Modifying
    @Query("UPDATE ImageEntity i SET i.status = 'D' WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId")
    void softDeleteAllByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    /**
     * 특정 이미지의 썸네일 여부 업데이트
     */
    @Modifying
    @Query("UPDATE ImageEntity i SET i.isThumbnail = :isThumbnail WHERE i.id = :id")
    void updateThumbnailStatus(@Param("id") Long id, @Param("isThumbnail") String isThumbnail);

    /**
     * 특정 참조의 모든 이미지 썸네일 해제 (새로운 썸네일 설정 시 기존 썸네일 해제용)
     */
    @Modifying
    @Query("UPDATE ImageEntity i SET i.isThumbnail = 'N' WHERE i.referenceType = :referenceType AND i.referenceId = :referenceId AND i.status = 'A'")
    void clearAllThumbnails(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);
}