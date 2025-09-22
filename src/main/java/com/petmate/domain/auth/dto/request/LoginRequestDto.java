package com.petmate.domain.auth.dto.request;

import lombok.Data;

@Data
public class LoginRequestDto {

    private String id;
    private String pw;

}
