package com.petmate.common.util;

import org.springframework.stereotype.Component;

import com.petmate.common.repository.CodeRepository;

@Component
public class CodeUtil {

    private final CodeRepository codeRepository;

    public CodeUtil(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    public String getCodeName(String groupCode, String code) {
        return codeRepository.findByGroupCodeAndCode(groupCode, code).getCodeNameKor();
    }

    public String getCodeNameEng(String groupCode, String code) {
        return codeRepository.findByGroupCodeAndCode(groupCode, code).getCodeNameEng();
    }

    public String getGenderName(String code) {
        return getCodeName("GENDER", code);
    }

    public String getUserRoleName(String code) {
        return getCodeName("USER_ROLE", code);
    }

    public String getUserStatusName(String code) {
        return getCodeName("USER_STATUS", code);
    }

    public String getServiceTypeName(String code) {
        return getCodeName("SERVICE_TYPE", code);
    }

    public String getSpeciesName(String code) {
        return getCodeName("SPECIES", code);
    }
}
