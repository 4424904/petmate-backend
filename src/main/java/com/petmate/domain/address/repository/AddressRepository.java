package com.petmate.domain.address.repository;

import com.petmate.domain.address.entity.AddressEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressEntity, Integer> {

    // 사용자의 주소 목록 조회 (생성일 순)
    List<AddressEntity> findByOwnerIdOrderByCreatedAtDesc(Integer ownerId);

    // 사용자의 기본 주소 조회
    Optional<AddressEntity> findByOwnerIdAndIsDefault(Integer ownerId, Integer isDefault);

    // 사용자의 기본 주소 조회(기본값)
    default Optional<AddressEntity> findDefaultAddressByOwnerId(Integer ownerId) {
        return findByOwnerIdAndIsDefault(ownerId, 1);
    }

    // 사용자의 주소 개수 조회
    Long countByOwnerId(Integer ownerId);

    // 사용자의 특정 라벨 주소 조회
    List<AddressEntity> findByOwnerIdAndLabel(Integer ownerId, String label);

    // 기존 기본 주소를 일반 주소로 변경
    @Modifying
    @Query(value = "UPDATE address SET IS_DEFAULT = 0 WHERE OWNER_ID = :ownerId AND IS_DEFAULT = 1", nativeQuery = true)
    void resetDefaultAddress(@Param("ownerId") Integer ownerId);

    // 특정 주소를 기본 주소로 설정
    @Modifying
    @Query(value = "UPDATE address SET IS_DEFAULT = 1 WHERE ID = :addressId AND OWNER_ID = :ownerId", nativeQuery = true)
    void setDefaultAddress(@Param("addressId") Integer addressId, @Param("ownerId") Integer ownerId);

    // 사용자별 주소 존재 여부 확인
    boolean existsByIdAndOwnerId(Integer id, Integer ownerId);

    // 사용자별 기본주소 존재 여부 확인
    AddressEntity findByIsDefaultAndOwnerId(Integer isDefault, Integer ownerId);

}
