package com.petmate.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmate.common.entity.CodeEntity;

@Repository
public interface CodeRepository extends JpaRepository<CodeEntity, Integer> {
    
    /**
     * 그룹 코드와 개별 코드로 조회
     */
    CodeEntity findByGroupCodeAndCode(String groupCode, String code);
    
    /**
     * 특정 그룹의 모든 코드를 정렬 순서로 조회
     */
    List<CodeEntity> findByGroupCodeOrderBySort(String groupCode);
    
    /**
     * 특정 그룹의 모든 코드를 정렬 순서로 조회 (활성 코드만)
     */
    List<CodeEntity> findByGroupCodeOrderBySortAsc(String groupCode);
    
    /**
     * 그룹 코드 존재 여부 확인
     */
    boolean existsByGroupCode(String groupCode);
    
    /**
     * 특정 그룹 코드에서 개별 코드 존재 여부 확인
     */
    boolean existsByGroupCodeAndCode(String groupCode, String code);
    
}
