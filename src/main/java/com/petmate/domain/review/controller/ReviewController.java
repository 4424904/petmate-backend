// src/main/java/com/petmate/domain/review/controller/ReviewController.java
package com.petmate.domain.review.controller;

import com.petmate.domain.review.dto.request.ReviewRequestDto;
import com.petmate.domain.review.dto.response.ReviewResponseDto;
import com.petmate.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 리뷰 생성: body = { reservationId, companyId, rating(1~5), comment, keywordIds? } */
    @PostMapping
    public ResponseEntity<ReviewResponseDto> create(
            @Valid @RequestBody ReviewRequestDto request,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Integer ownerUserId = Integer.parseInt(principal.getName()); // JWT sub

        ReviewResponseDto dto = reviewService.createReview(request, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /** 특정 예약의 내 리뷰 조회 (없으면 404) */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ReviewResponseDto> getMyByReservation(
            @PathVariable Integer reservationId,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Integer ownerUserId = Integer.parseInt(principal.getName());

        ReviewResponseDto dto = reviewService.getMyReviewByReservation(reservationId, ownerUserId);
        return ResponseEntity.ok(dto);
    }

    /** 내 리뷰 목록 (옵션: companyId, page, size) */
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Integer ownerUserId = Integer.parseInt(principal.getName());

        List<ReviewResponseDto> list = reviewService.getMyReviews(ownerUserId, companyId, page, size);
        return ResponseEntity.ok(list);
    }

    /** 리뷰 삭제(소유자만) */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer reviewId,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Integer ownerUserId = Integer.parseInt(principal.getName());

        reviewService.deleteMyReview(reviewId, ownerUserId);
        return ResponseEntity.noContent().build();
    }
}
