package com.petmate.domain.test.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.test.service.JpaTestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test/jpa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class JpaTestController {

    private final JpaTestService jpaTestService;

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<UserEntity>> getAllTests() {
        log.info("JPA 테스트 전체 조회 요청");
        List<UserEntity> tests = jpaTestService.findAll();
        return ResponseEntity.ok(tests);
    }

    // ID로 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getTestById(@PathVariable Integer id) {
        log.info("JPA 테스트 ID {} 조회 요청", id);
        UserEntity test = jpaTestService.findById(id);
        return test != null ? ResponseEntity.ok(test) : ResponseEntity.notFound().build();
    }

    // 생성
    @PostMapping
    public ResponseEntity<UserEntity> createTest(@RequestBody Map<String, String> request) {
        log.info("JPA 테스트 생성 요청: {}", request);
        
        String name = request.get("name");
        String description = request.get("description");
        
        UserEntity createdTest = jpaTestService.createTest(name, description);
        return ResponseEntity.ok(createdTest);
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> updateTest(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        log.info("JPA 테스트 ID {} 수정 요청: {}", id, request);
        
        String name = request.get("name");
        String description = request.get("description");
        
        UserEntity updatedTest = jpaTestService.updateTest(id, name, description);
        return updatedTest != null ? ResponseEntity.ok(updatedTest) : ResponseEntity.notFound().build();
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Integer id) {
        log.info("JPA 테스트 ID {} 삭제 요청", id);
        boolean deleted = jpaTestService.deleteTest(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 이메일로 조회
    @GetMapping("/email/{email}")
    public ResponseEntity<UserEntity> getTestByEmail(@PathVariable String email) {
        log.info("JPA 테스트 이메일 {} 조회 요청", email);
        UserEntity test = jpaTestService.findByEmail(email);
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