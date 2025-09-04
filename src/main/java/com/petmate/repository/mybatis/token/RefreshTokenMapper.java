package com.petmate.repository.mybatis.token;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

    void saveToken(@Param("ownerNo") int ownerNo,
                   @Param("refreshToken") String refreshToken);

    void deleteByToken(@Param("refreshToken") String refreshToken);
}

