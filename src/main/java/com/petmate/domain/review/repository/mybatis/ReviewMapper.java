// src/main/java/com/petmate/domain/review/repository/mybatis/ReviewMapper.java
package com.petmate.domain.review.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Mapper
public interface ReviewMapper {

    void insertReviewKeywords(@Param("reviewId") Integer reviewId,
                              @Param("keywordIds") List<Integer> keywordIds);

    void deleteKeywordsByReviewId(@Param("reviewId") Integer reviewId);

    List<KeywordRow> selectKeywordsByReviewId(@Param("reviewId") Integer reviewId);

    List<KeywordWithReviewRow> selectKeywordsByReviewIds(@Param("reviewIds") List<Integer> reviewIds);

    @Getter @Setter
    class KeywordRow {
        private Integer id;
        private String label;
        private String category;
        private String serviceType;
    }

    @Getter @Setter
    class KeywordWithReviewRow {
        private Integer reviewId;
        private Integer id;
        private String label;
        private String category;
        private String serviceType;
    }
}
