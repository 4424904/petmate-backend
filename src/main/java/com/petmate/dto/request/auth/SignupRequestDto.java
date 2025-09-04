package com.petmate.dto.request.auth;

import lombok.Data;

@Data
public class SignupRequestDto {

    private String id;
    private String pw;
    private String mail;

}
