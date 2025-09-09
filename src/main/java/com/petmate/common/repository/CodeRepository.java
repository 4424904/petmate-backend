package com.petmate.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmate.common.entity.CodeEntity;

@Repository
public interface CodeRepository extends JpaRepository<CodeEntity, Integer> {
    

    CodeEntity findByGroupCodeAndCode(String groupCode, String code);
    
}
