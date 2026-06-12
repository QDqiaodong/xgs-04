package com.bookexchange.controller;

import com.bookexchange.dto.BookRankDTO;
import com.bookexchange.dto.BorrowTrendDTO;
import com.bookexchange.dto.CategoryRatioDTO;
import com.bookexchange.dto.CityActivityDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.dto.StatisticsQueryDTO;
import com.bookexchange.dto.UserBorrowRankDTO;
import com.bookexchange.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @PostMapping("/borrow-trend")
    public Result<List<BorrowTrendDTO>> getBorrowTrend(@RequestBody StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getBorrowTrend(queryDTO));
    }

    @PostMapping("/category-ratio")
    public Result<List<CategoryRatioDTO>> getCategoryRatio(@RequestBody StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getCategoryRatio(queryDTO));
    }

    @PostMapping("/city-rank")
    public Result<List<CityActivityDTO>> getCityActivityRank(@RequestBody StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getCityActivityRank(queryDTO));
    }

    @PostMapping("/hot-books")
    public Result<List<BookRankDTO>> getHotBooksRank(@RequestBody StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getHotBooksRank(queryDTO));
    }

    @PostMapping("/user-rank")
    public Result<List<UserBorrowRankDTO>> getUserBorrowRank(@RequestBody StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getUserBorrowRank(queryDTO));
    }

    @PostMapping("/overview")
    public Result<Map<String, Object>> getStatisticsOverview(@RequestBody StatisticsQueryDTO queryDTO) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("borrowTrend", statisticsService.getBorrowTrend(queryDTO));
        overview.put("categoryRatio", statisticsService.getCategoryRatio(queryDTO));
        overview.put("cityRank", statisticsService.getCityActivityRank(queryDTO));
        overview.put("hotBooks", statisticsService.getHotBooksRank(queryDTO));
        overview.put("userRank", statisticsService.getUserBorrowRank(queryDTO));
        return Result.success(overview);
    }
}
