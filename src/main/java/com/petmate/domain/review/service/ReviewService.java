// src/main/java/com/petmate/domain/review/service/ReviewService.java
package com.petmate.domain.review.service;

import com.petmate.domain.company.repository.CompanyRepository;
import com.petmate.domain.review.dto.request.ReviewRequestDto;
import com.petmate.domain.review.dto.response.ReviewResponseDto;
import com.petmate.domain.review.entity.ReviewEntity;
import com.petmate.domain.review.repository.jpa.ReviewRepository;
import com.petmate.domain.review.repository.mybatis.ReviewMapper;
import com.petmate.domain.booking.entity.BookingEntity;
import com.petmate.domain.booking.repository.jpa.BookingRepository;
import com.petmate.domain.user.repository.jpa.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ReviewMapper reviewMapper;

    private static final String RESV_STATUS_COMPLETED = "1";

    private void validateRequest(ReviewRequestDto req) {
        if (req == null) throw new IllegalArgumentException("요청이 없습니다.");
        if (req.getReservationId() == null || req.getReservationId() <= 0)
            throw new IllegalArgumentException("예약 ID가 유효하지 않습니다.");
        if (req.getCompanyId() == null || req.getCompanyId() <= 0)
            throw new IllegalArgumentException("업체 ID가 유효하지 않습니다.");
        Integer rating = req.getRating();
        if (rating == null || rating < 1 || rating > 5)
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        if (req.getComment() == null || req.getComment().isBlank())
            throw new IllegalArgumentException("코멘트는 필수입니다.");
        if (req.getComment().length() > 1000)
            throw new IllegalArgumentException("코멘트 길이가 너무 깁니다.");
        if (req.getKeywordIds() != null && req.getKeywordIds().stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("키워드 ID에 null이 포함되어 있습니다.");
    }

    @Transactional
    public ReviewResponseDto createReview(ReviewRequestDto req, Integer ownerUserId) {
        validateRequest(req);

        BookingEntity booking = bookingRepository.findById(req.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
        if (!Objects.equals(booking.getOwnerUserId(), ownerUserId))
            throw new IllegalStateException("본인 예약만 리뷰 작성 가능");
        if (!RESV_STATUS_COMPLETED.equals(booking.getStatus()))
            throw new IllegalStateException("완료된 예약만 리뷰 작성 가능");
        if (!Objects.equals(booking.getCompanyId(), req.getCompanyId()))
            throw new IllegalStateException("예약의 업체와 요청 업체가 다릅니다.");

        if (reviewRepository.existsByReservation_Id(req.getReservationId()))
            throw new IllegalStateException("해당 예약의 리뷰가 이미 존재합니다.");

        var userRef = userRepository.getReferenceById(ownerUserId.longValue());
        var compRef = companyRepository.getReferenceById(req.getCompanyId());
        var bookRef = bookingRepository.getReferenceById(req.getReservationId());

        ReviewEntity saved = reviewRepository.save(
                ReviewEntity.builder()
                        .reservation(bookRef)
                        .ownerUser(userRef)
                        .company(compRef)
                        .rating(req.getRating())
                        .comment(req.getComment())
                        .isVisible(true)
                        .build()
        );

        List<Integer> kwIds = req.getKeywordIds();
        if (kwIds != null && !kwIds.isEmpty()) {
            reviewMapper.insertReviewKeywords(saved.getId(), kwIds);
        }

        var kws = reviewMapper.selectKeywordsByReviewId(saved.getId()).stream()
                .map(k -> new ReviewResponseDto.KeywordDto(
                        k.getId(), k.getLabel(), k.getCategory(), k.getServiceType()
                ))
                .toList();

        return ReviewResponseDto.from(saved, kws);
    }

    @Transactional
    public ReviewResponseDto getMyReviewByReservation(Integer reservationId, Integer ownerUserId) {
        ReviewEntity r = reviewRepository
                .findByReservation_IdAndOwnerUser_Id(reservationId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없습니다."));

        var kws = reviewMapper.selectKeywordsByReviewId(r.getId()).stream()
                .map(k -> new ReviewResponseDto.KeywordDto(
                        k.getId(), k.getLabel(), k.getCategory(), k.getServiceType()
                ))
                .toList();

        return ReviewResponseDto.from(r, kws);
    }

    @Transactional
    public List<ReviewResponseDto> getMyReviews(Integer ownerUserId, Integer companyId, int page, int size) {
        var pageable = PageRequest.of(page, size);

        List<ReviewEntity> reviews = (companyId == null)
                ? reviewRepository.findByOwnerUser_IdOrderByCreatedAtDesc(ownerUserId, pageable)
                : reviewRepository.findByOwnerUser_IdAndCompany_IdOrderByCreatedAtDesc(ownerUserId, companyId, pageable);

        if (reviews.isEmpty()) return List.of();

        List<Integer> rids = reviews.stream().map(ReviewEntity::getId).toList();

        var rows = reviewMapper.selectKeywordsByReviewIds(rids);
        Map<Integer, List<ReviewResponseDto.KeywordDto>> kwMap = rows.stream()
                .collect(Collectors.groupingBy(
                        ReviewMapper.KeywordWithReviewRow::getReviewId,
                        Collectors.mapping(
                                k -> new ReviewResponseDto.KeywordDto(
                                        k.getId(), k.getLabel(), k.getCategory(), k.getServiceType()
                                ),
                                Collectors.toList()
                        )
                ));

        return reviews.stream()
                .map(r -> ReviewResponseDto.from(r, kwMap.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void deleteMyReview(Integer reviewId, Integer ownerUserId) {
        ReviewEntity r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 없습니다."));
        Long ownerId = r.getOwnerUser().getId();
        if (!Objects.equals(ownerId, ownerUserId.longValue()))
            throw new IllegalStateException("본인 리뷰만 삭제할 수 있습니다.");

        reviewMapper.deleteKeywordsByReviewId(reviewId);
        reviewRepository.delete(r);
    }

    @Transactional
    public List<ReviewResponseDto> getReviewsByCompany(Integer companyId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ReviewEntity> pg = reviewRepository.findByCompany_Id(companyId, pr);
        List<ReviewEntity> list = pg.getContent();
        if (list.isEmpty()) return List.of();

        List<Integer> rids = list.stream().map(ReviewEntity::getId).toList();

        var rows = reviewMapper.selectKeywordsByReviewIds(rids);
        Map<Integer, List<ReviewResponseDto.KeywordDto>> kwMap = rows.stream()
                .collect(Collectors.groupingBy(
                        ReviewMapper.KeywordWithReviewRow::getReviewId,
                        Collectors.mapping(
                                k -> new ReviewResponseDto.KeywordDto(
                                        k.getId(), k.getLabel(), k.getCategory(), k.getServiceType()
                                ),
                                Collectors.toList()
                        )
                ));

        return list.stream()
                .map(r -> ReviewResponseDto.from(r, kwMap.getOrDefault(r.getId(), List.of())))
                .toList();
    }
}
