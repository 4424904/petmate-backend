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


    public String getUserStatusName(String code) {
        return getCodeName("USER_STATUS", code);
    }

    public String getUserRoleName(String code) {
        return getCodeName("USER_ROLE", code);
    }

    public String getSocialProviderName(String code) {
        return getCodeName("SOCIAL_PROVIDER", code);
    }

    public String getAddressLabelName(String code) {
        return getCodeName("ADDRESS_LABEL", code);
    }

    public String getGenderName(String code) {
        return getCodeName("GENDER", code);
    }

    public String getSpeciesName(String code) {
        return getCodeName("SPECIES", code);
    }

    public String getCompanyTypeName(String code) {
        return getCodeName("COMPANY_TYPE", code);
    }

    public String getCompanyStatusName(String code) {
        return getCodeName("COMPANY_STATUS", code);
    }

    public String getServiceTypeName(String code) {
        return getCodeName("SERVICE_TYPE", code);
    }

    public String getReservationStatusName(String code) {
        return getCodeName("RESERVATION_STATUS", code);
    }

    public String getPaymentStatusName(String code) {
        return getCodeName("PAYMENT_STATUS", code);
    }

    public String getPaymentProviderName(String code) {
        return getCodeName("PAYMENT_PROVIDER", code);
    }

    public String getNotificationTypeName(String code) {
        return getCodeName("NOTIFICATION_TYPE", code);
    }

    public String getNotificationChannelName(String code) {
        return getCodeName("NOTIFICATION_CHANNEL", code);
    }

    public String getNotificationStatusName(String code) {
        return getCodeName("NOTIFICATION_STATUS", code);
    }

}
