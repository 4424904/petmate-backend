package com.petmate.repository.mybatis.user;

import com.petmate.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {

    // 회원 조회
    MemberDto findById(String id);

    // 회원가입
    int signup(MemberDto member);
}
