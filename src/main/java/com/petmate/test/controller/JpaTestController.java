package com.petmate.test.controller;

import com.petmate.entity.test.JpaTest;
import com.petmate.test.service.JpaTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/jpa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class JpaTestController {

    private final JpaTestService jpaTestService;

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<JpaTest>> getAllTests() {
        log.info("JPA 테스트 전체 조회 요청");
        List<JpaTest> tests = jpaTestService.findAll();
        return ResponseEntity.ok(tests);
    }

    // ID로 조회
    @GetMapping("/{id}")
    public ResponseEntity<JpaTest> getTestById(@PathVariable Long id) {
        log.info("JPA 테스트 ID {} 조회 요청", id);
        JpaTest test = jpaTestService.findById(id);
        return test != null ? ResponseEntity.ok(test) : ResponseEntity.notFound().build();
    }

    // 생성
    @PostMapping
    public ResponseEntity<JpaTest> createTest(@RequestBody Map<String, String> request) {
        log.info("JPA 테스트 생성 요청: {}", request);
        
        String name = request.get("name");
        String description = request.get("description");
        
        JpaTest createdTest = jpaTestService.createTest(name, description);
        return ResponseEntity.ok(createdTest);
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<JpaTest> updateTest(@PathVariable Long id, @RequestBody Map<String, String> request) {
        log.info("JPA 테스트 ID {} 수정 요청: {}", id, request);
        
        String name = request.get("name");
        String description = request.get("description");
        
        JpaTest updatedTest = jpaTestService.updateTest(id, name, description);
        return updatedTest != null ? ResponseEntity.ok(updatedTest) : ResponseEntity.notFound().build();
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        log.info("JPA 테스트 ID {} 삭제 요청", id);
        boolean deleted = jpaTestService.deleteTest(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 이메일로 조회
    @GetMapping("/email/{email}")
    public ResponseEntity<JpaTest> getTestByEmail(@PathVariable String email) {
        log.info("JPA 테스트 이메일 {} 조회 요청", email);
        JpaTest test = jpaTestService.findByEmail(email);
        return test != null ? ResponseEntity.ok(test) : ResponseEntity.notFound().build();
    }

    // 테스트 데이터 초기화 (개발용)
    @PostMapping("/init")
    public ResponseEntity<String> initTestData() {
        log.info("JPA 테스트 데이터 초기화 요청");
        jpaTestService.initTestData();
        return ResponseEntity.ok("테스트 데이터가 초기화되었습니다.");
    }
}