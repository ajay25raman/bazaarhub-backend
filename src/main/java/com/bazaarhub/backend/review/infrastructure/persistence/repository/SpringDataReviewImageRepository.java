package com.bazaarhub.backend.review.infrastructure.persistence.repository;

import com.bazaarhub.backend.review.infrastructure.persistence.entity.ReviewImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataReviewImageRepository extends JpaRepository<ReviewImageEntity, Long> {

    List<ReviewImageEntity> findByReviewIdOrderByDisplayOrderAscIdAsc(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
