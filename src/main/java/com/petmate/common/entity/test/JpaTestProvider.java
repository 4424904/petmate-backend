package com.petmate.common.entity.test;

public enum JpaTestProvider {
    NAVER("네이버"),
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String description;

    JpaTestProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}