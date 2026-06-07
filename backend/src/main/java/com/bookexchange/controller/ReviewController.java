package com.bookexchange.controller;

import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.Result;
import com.bookexchange.dto.ReviewDTO;
import com.bookexchange.dto.ReviewQueryDTO;
import com.bookexchange.entity.Review;
import com.bookexchange.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/query")
    public Result<PageResult<Review>> queryReviews(@RequestBody ReviewQueryDTO queryDTO) {
        Page<Review> page = reviewService.queryReviews(queryDTO);
        PageResult<Review> result = new PageResult<>();
        result.setList(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPageNum(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        return Result.success(result);
    }

    @PostMapping
    public Result<Review> createReview(@RequestBody ReviewDTO dto) {
        Review review = reviewService.createReview(dto);
        return review != null ? Result.success(review) : Result.error("评价失败，请检查参数或是否已评价过");
    }

    @GetMapping("/{id}")
    public Result<Review> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getReviewById(id);
        return review != null ? Result.success(review) : Result.error("评价不存在");
    }

    @GetMapping("/book/{bookId}")
    public Result<List<Review>> getReviewsByBookId(@PathVariable Long bookId) {
        return Result.success(reviewService.getReviewsByBookId(bookId));
    }

    @GetMapping("/book/{bookId}/stats")
    public Result<Map<String, Object>> getBookReviewStats(@PathVariable Long bookId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", reviewService.getAverageRatingByBookId(bookId));
        stats.put("reviewCount", reviewService.getReviewCountByBookId(bookId));
        return Result.success(stats);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Review>> getReviewsByUserId(@PathVariable Long userId) {
        return Result.success(reviewService.getReviewsByUserId(userId));
    }

    @GetMapping("/borrow-record/{borrowRecordId}")
    public Result<Review> getReviewByBorrowRecordId(@PathVariable Long borrowRecordId) {
        Review review = reviewService.getReviewByBorrowRecordId(borrowRecordId);
        return review != null ? Result.success(review) : Result.success(null);
    }

    @GetMapping("/borrow-record/{borrowRecordId}/exists")
    public Result<Boolean> hasReviewedBorrowRecord(@PathVariable Long borrowRecordId) {
        return Result.success(reviewService.hasReviewedBorrowRecord(borrowRecordId));
    }
}
