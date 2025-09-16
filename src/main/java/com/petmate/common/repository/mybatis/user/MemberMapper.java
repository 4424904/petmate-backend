// src/main/java/com/petmate/common/repository/mybatis/user/MemberMapper.java
package com.petmate.common.repository.mybatis.user;

import com.petmate.domain.auth.dto.response.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {
    MemberDto findById(@Param("id") String id);
    void signup(MemberDto member);
}
