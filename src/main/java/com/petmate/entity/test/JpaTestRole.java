package com.petmate.entity.test;

public enum JpaTestRole {
    GUEST("비회원"),
    OWNER("반려인"),
    PETMATE("펫메이트"),
    ADMIN("관리자");

    private final String description;

    JpaTestRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}