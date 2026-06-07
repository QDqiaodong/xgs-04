package com.bookexchange.service;

import com.bookexchange.dto.ReviewDTO;
import com.bookexchange.dto.ReviewQueryDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.entity.Review;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.ReviewRepository;
import com.bookexchange.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public Page<Review> queryReviews(ReviewQueryDTO queryDTO) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        if ("rating".equals(queryDTO.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "rating");
        } else if ("rating_asc".equals(queryDTO.getSortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "rating");
        } else if ("oldest".equals(queryDTO.getSortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "createTime");
        }

        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            sort
        );

        Specification<Review> spec = buildSpecification(queryDTO);
        return reviewRepository.findAll(spec, pageable);
    }

    private Specification<Review> buildSpecification(ReviewQueryDTO queryDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getBookId() != null) {
                predicates.add(cb.equal(root.get("book").get("id"), queryDTO.getBookId()));
            }

            if (queryDTO.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), queryDTO.getUserId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Review createReview(ReviewDTO dto) {
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            return null;
        }

        Book book = bookRepository.findById(dto.getBookId()).orElse(null);
        User user = userRepository.findById(dto.getUserId()).orElse(null);

        if (book == null || user == null) {
            return null;
        }

        BorrowRecord borrowRecord = null;
        if (dto.getBorrowRecordId() != null) {
            borrowRecord = borrowRecordRepository.findById(dto.getBorrowRecordId()).orElse(null);
            if (borrowRecord != null && !"RETURNED".equals(borrowRecord.getStatus())) {
                return null;
            }
            if (reviewRepository.existsByBorrowRecordId(dto.getBorrowRecordId())) {
                return null;
            }
        }

        if (reviewRepository.existsByBookIdAndUserId(dto.getBookId(), dto.getUserId())) {
            return null;
        }

        Review review = new Review();
        review.setBook(book);
        review.setUser(user);
        review.setBorrowRecord(borrowRecord);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());

        Review saved = reviewRepository.save(review);

        if (borrowRecord != null) {
            borrowRecord.setReviewed(true);
            borrowRecordRepository.save(borrowRecord);
        }

        return saved;
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreateTimeDesc(bookId);
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public Review getReviewByBorrowRecordId(Long borrowRecordId) {
        return reviewRepository.findByBorrowRecordId(borrowRecordId).orElse(null);
    }

    public boolean hasReviewedBorrowRecord(Long borrowRecordId) {
        return reviewRepository.existsByBorrowRecordId(borrowRecordId);
    }

    public Double getAverageRatingByBookId(Long bookId) {
        List<Review> reviews = reviewRepository.findByBookIdOrderByCreateTimeDesc(bookId);
        if (reviews.isEmpty()) {
            return null;
        }
        double sum = reviews.stream().mapToInt(Review::getRating).sum();
        return Math.round(sum / reviews.size() * 10.0) / 10.0;
    }

    public Integer getReviewCountByBookId(Long bookId) {
        return (int) reviewRepository.findByBookIdOrderByCreateTimeDesc(bookId).size();
    }
}
