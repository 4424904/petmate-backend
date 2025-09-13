package com.petmate.common.util;

import org.springframework.stereotype.Component;

import com.petmate.common.entity.CodeEntity;
import com.petmate.common.repository.CodeRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CodeUtil {

    private final CodeRepository codeRepository;

    public CodeUtil(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    public String getCodeName(String groupCode, String code) {
        CodeEntity codeEntity = codeRepository.findByGroupCodeAndCode(groupCode, code);
        if (codeEntity == null) {
            return "";
        }
        return codeEntity.getCodeNameKor();
    }

    public String getCodeNameEng(String groupCode, String code) {
        CodeEntity codeEntity = codeRepository.findByGroupCodeAndCode(groupCode, code);
        if (codeEntity == null) {
            return "";
        }
        return codeEntity.getCodeNameEng();
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

    public String getImageTypeName(String code) {
        return getCodeName("IMAGE_TYPE", code);
    }

    // ================================
    // 코드 리스트 조회 메서드들
    // ================================
    
    /**
     * 특정 그룹의 모든 코드 엔티티 조회 (정렬 순서대로)
     */
    public List<CodeEntity> getCodeList(String groupCode) {
        return codeRepository.findByGroupCodeOrderBySort(groupCode);
    }
    
    /**
     * 특정 그룹의 코드를 Map 형태로 조회 (code -> codeNameKor)
     */
    public Map<String, String> getCodeMap(String groupCode) {
        return getCodeList(groupCode).stream()
                .collect(Collectors.toMap(
                    CodeEntity::getCode,
                    CodeEntity::getCodeNameKor
                ));
    }
    
    /**
     * 특정 그룹의 코드를 영문 Map 형태로 조회 (code -> codeNameEng)
     */
    public Map<String, String> getCodeMapEng(String groupCode) {
        return getCodeList(groupCode).stream()
                .collect(Collectors.toMap(
                    CodeEntity::getCode,
                    CodeEntity::getCodeNameEng
                ));
    }
    
    /**
     * 코드 존재 여부 확인
     */
    public boolean isValidCode(String groupCode, String code) {
        return codeRepository.existsByGroupCodeAndCode(groupCode, code);
    }
    
    /**
     * 그룹 코드 존재 여부 확인
     */
    public boolean isValidGroupCode(String groupCode) {
        return codeRepository.existsByGroupCode(groupCode);
    }

    // ================================
    // 특정 그룹 코드 리스트 조회 편의 메서드들
    // ================================
    
    /**
     * 사용자 상태 코드 리스트 조회
     */
    public List<CodeEntity> getUserStatusList() {
        return getCodeList("USER_STATUS");
    }
    
    /**
     * 사용자 역할 코드 리스트 조회
     */
    public List<CodeEntity> getUserRoleList() {
        return getCodeList("USER_ROLE");
    }
    
    /**
     * 소셜 제공자 코드 리스트 조회
     */
    public List<CodeEntity> getSocialProviderList() {
        return getCodeList("SOCIAL_PROVIDER");
    }
    
    /**
     * 주소 라벨 코드 리스트 조회
     */
    public List<CodeEntity> getAddressLabelList() {
        return getCodeList("ADDRESS_LABEL");
    }
    
    /**
     * 성별 코드 리스트 조회
     */
    public List<CodeEntity> getGenderList() {
        return getCodeList("GENDER");
    }
    
    /**
     * 동물 종류 코드 리스트 조회
     */
    public List<CodeEntity> getSpeciesList() {
        return getCodeList("SPECIES");
    }
    
    /**
     * 업체 타입 코드 리스트 조회
     */
    public List<CodeEntity> getCompanyTypeList() {
        return getCodeList("COMPANY_TYPE");
    }
    
    /**
     * 업체 상태 코드 리스트 조회
     */
    public List<CodeEntity> getCompanyStatusList() {
        return getCodeList("COMPANY_STATUS");
    }
    
    /**
     * 서비스 타입 코드 리스트 조회
     */
    public List<CodeEntity> getServiceTypeList() {
        return getCodeList("SERVICE_TYPE");
    }
    
    /**
     * 예약 상태 코드 리스트 조회
     */
    public List<CodeEntity> getReservationStatusList() {
        return getCodeList("RESERVATION_STATUS");
    }
    
    /**
     * 결제 상태 코드 리스트 조회
     */
    public List<CodeEntity> getPaymentStatusList() {
        return getCodeList("PAYMENT_STATUS");
    }
    
    /**
     * 결제 제공자 코드 리스트 조회
     */
    public List<CodeEntity> getPaymentProviderList() {
        return getCodeList("PAYMENT_PROVIDER");
    }
    
    /**
     * 알림 타입 코드 리스트 조회
     */
    public List<CodeEntity> getNotificationTypeList() {
        return getCodeList("NOTIFICATION_TYPE");
    }
    
    /**
     * 알림 채널 코드 리스트 조회
     */
    public List<CodeEntity> getNotificationChannelList() {
        return getCodeList("NOTIFICATION_CHANNEL");
    }
    
    /**
     * 알림 상태 코드 리스트 조회
     */
    public List<CodeEntity> getNotificationStatusList() {
        return getCodeList("NOTIFICATION_STATUS");
    }
    
    /**
     * 이미지 타입 코드 리스트 조회
     */
    public List<CodeEntity> getImageTypeList() {
        return getCodeList("IMAGE_TYPE");
    }

}
