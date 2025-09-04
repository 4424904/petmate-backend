package com.petmate.dto.request.auth;

import lombok.Data;

@Data
public class LoginRequestDto {

    private String id;
    private String pw;

}
