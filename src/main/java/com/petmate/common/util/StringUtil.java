package com.petmate.common.util;

/**
 * 문자열 관련 유틸리티 클래스
 */
public class StringUtil {

    /**
     * 영문 코드명을 camelCase로 변환
     * 예: "USER_PROFILE" -> "userProfile"
     *     "PETMATE_CERT" -> "petmateCert"
     */
    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        String[] words = str.toLowerCase().split("_");
        StringBuilder camelCase = new StringBuilder(words[0]);
        
        for (int i = 1; i < words.length; i++) {
            if (words[i].length() > 0) {
                camelCase.append(words[i].substring(0, 1).toUpperCase())
                        .append(words[i].substring(1));
            }
        }
        
        return camelCase.toString();
    }
    
    /**
     * 문자열을 PascalCase로 변환
     * 예: "user_profile" -> "UserProfile"
     */
    public static String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        String camelCase = toCamelCase(str);
        if (camelCase.length() > 0) {
            return camelCase.substring(0, 1).toUpperCase() + camelCase.substring(1);
        }
        return camelCase;
    }
    
    /**
     * camelCase를 snake_case로 변환
     * 예: "userProfile" -> "user_profile"
     */
    public static String toSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * 문자열을 kebab-case로 변환
     * 예: "userProfile" -> "user-profile"
     */
    public static String toKebabCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
    
    /**
     * 문자열이 null이거나 비어있는지 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 비어있지 않은지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 첫 글자를 대문자로 변환
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * 첫 글자를 소문자로 변환
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private StringUtil() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
}