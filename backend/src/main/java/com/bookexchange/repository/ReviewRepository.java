package com.bookexchange.repository;

import com.bookexchange.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findByBookIdOrderByCreateTimeDesc(Long bookId);

    Page<Review> findByBookId(Long bookId, Pageable pageable);

    Optional<Review> findByBorrowRecordId(Long borrowRecordId);

    boolean existsByBorrowRecordId(Long borrowRecordId);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    List<Review> findByUserIdOrderByCreateTimeDesc(Long userId);
}
