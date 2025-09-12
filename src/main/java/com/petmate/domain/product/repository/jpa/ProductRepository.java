package com.petmate.domain.product.repository.jpa;

import com.petmate.domain.product.entity.ProductEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

    List<ProductEntity> findByCompanyId(Integer companyId);
    List<ProductEntity> findByServiceType(String serviceType);
    List<ProductEntity> findByIsActive(Integer isActive);

    @Query("SELECT p FROM ProductEntity p WHERE p.companyId = :companyId AND p.isActive = 1")
    List<ProductEntity> findActiveProductsByCompany(@Param("companyId") Integer companyId);

    // 가격 범위 검색
    List<ProductEntity> findByPriceBetweenAndIsActive(Integer minPrice, Integer maxPrice, Integer isActive);
}
