package com.petmate.payment.repository.jpa;

import com.petmate.payment.entity.GroupCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommonCodeRepository extends JpaRepository<GroupCodeEntity, Integer> {

    List<GroupCodeEntity> findByCodeTypeAndUseYnOrderBySortOrder(String codeType, String useYn);

    Optional<GroupCodeEntity> findByCodeTypeAndCodeValue(String codeType, String codeValue);

    Optional<GroupCodeEntity> findByCodeTypeAndCodeName(String codeType, String codeName);



}
