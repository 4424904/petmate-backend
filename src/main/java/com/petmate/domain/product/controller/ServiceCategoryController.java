package com.petmate.domain.product.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-categories")
@RequiredArgsConstructor
@Slf4j
public class ServiceCategoryController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getServiceCategories() {
        log.info("서비스 카테고리 목록 조회 요청");

        List<Map<String, Object>> categories = Arrays.asList(
                createCategory("P", "펜션"),
                createCategory("H", "호텔"),
                createCategory("C", "카페"),
                createCategory("R", "식당")
        );

        log.info("서비스 카테고리 {} 개 반환", categories.size());
        return ResponseEntity.ok(categories);
    }

    private Map<String, Object> createCategory(String id, String name) {
        Map<String, Object> category = new HashMap<>();
        category.put("id", id);
        category.put("name", name);
        return category;
    }

}
