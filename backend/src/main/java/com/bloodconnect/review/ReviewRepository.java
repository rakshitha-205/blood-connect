package com.bloodconnect.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    interface RatingRow {
        UUID getUserId();
        Double getAverage();
        long getTotal();
    }

    @Query("""
            select r.reviewee.id as userId, avg(r.rating) as average, count(r) as total
            from Review r where r.reviewee.id in :userIds group by r.reviewee.id""")
    List<RatingRow> ratingsFor(@Param("userIds") Collection<UUID> userIds);

    boolean existsByRequestIdAndReviewerIdAndRevieweeId(UUID requestId, UUID reviewerId, UUID revieweeId);

    List<Review> findAllByRequestIdAndReviewerId(UUID requestId, UUID reviewerId);

    List<Review> findTop20ByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId);
}
