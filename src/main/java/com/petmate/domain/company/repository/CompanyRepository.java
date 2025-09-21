package com.petmate.domain.company.repository;

import com.petmate.domain.company.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Integer> {

    // 특정 사용자가 등록한 업체 목록 등록일 내림차순으로 조회
    List<CompanyEntity> findByCreatedByOrderByCreatedAtDesc(Integer createdBy);

    // 업체 아이디와 등록자 아이디로 업체 조회
    Optional<CompanyEntity> findByIdAndCreatedBy(Integer id, Integer createdBy);

    // 사업자등록번호 중복 체크
    boolean existsByBizRegNo(String bizRegNo);

    // 개인 업체 등록 여부 확인 (createdBy + type 기반)
    boolean existsByCreatedByAndType(Integer createdBy, String type);

    // 특정 상태의 업체 목록을 등록일 내림차순으로 조회(관리자용)
    List<CompanyEntity> findByStatusOrderByCreatedAtDesc(String status);
}
