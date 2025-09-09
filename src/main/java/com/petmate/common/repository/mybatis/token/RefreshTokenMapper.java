package com.petmate.common.repository.mybatis.token;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
    void saveToken(@Param("memberNo") long memberNo, @Param("token") String token);
    void deleteByToken(@Param("token") String token);
}
