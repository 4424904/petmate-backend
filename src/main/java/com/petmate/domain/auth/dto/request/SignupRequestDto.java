package com.petmate.domain.auth.dto.request;

import lombok.Data;

@Data
public class SignupRequestDto {

    private String id;
    private String pw;
    private String mail;

}
