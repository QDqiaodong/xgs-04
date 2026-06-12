package com.bookexchange.service;

import com.bookexchange.dto.BookRankDTO;
import com.bookexchange.dto.BorrowTrendDTO;
import com.bookexchange.dto.CategoryRatioDTO;
import com.bookexchange.dto.CityActivityDTO;
import com.bookexchange.dto.StatisticsQueryDTO;
import com.bookexchange.dto.UserBorrowRankDTO;
import com.bookexchange.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STATISTICS_CACHE_KEY = "statistics:";
    private static final long CACHE_EXPIRE_HOURS = 1;

    public List<BorrowTrendDTO> getBorrowTrend(StatisticsQueryDTO queryDTO) {
        String cacheKey = STATISTICS_CACHE_KEY + "trend:" + buildCacheKey(queryDTO);
        @SuppressWarnings("unchecked")
        List<BorrowTrendDTO> cached = (List<BorrowTrendDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime startTime = getStartTime(queryDTO.getStartDate());
        LocalDateTime endTime = getEndTime(queryDTO.getEndDate());
        String dimension = StringUtils.hasText(queryDTO.getDimension()) ? queryDTO.getDimension() : "day";

        List<Object[]> results;
        switch (dimension.toLowerCase()) {
            case "week":
                results = borrowRecordRepository.countBorrowsByWeek(startTime, endTime);
                break;
            case "month":
                results = borrowRecordRepository.countBorrowsByMonth(startTime, endTime);
                break;
            case "day":
            default:
                results = borrowRecordRepository.countBorrowsByDay(startTime, endTime);
                break;
        }

        List<BorrowTrendDTO> trendList = results.stream()
            .map(row -> new BorrowTrendDTO(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue()
            ))
            .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, trendList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return trendList;
    }

    public List<CategoryRatioDTO> getCategoryRatio(StatisticsQueryDTO queryDTO) {
        String cacheKey = STATISTICS_CACHE_KEY + "category:" + buildCacheKey(queryDTO);
        @SuppressWarnings("unchecked")
        List<CategoryRatioDTO> cached = (List<CategoryRatioDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime startTime = getStartTime(queryDTO.getStartDate());
        LocalDateTime endTime = getEndTime(queryDTO.getEndDate());

        List<Object[]> results = borrowRecordRepository.countBorrowsByCategory(startTime, endTime);

        long totalBorrows = results.stream()
            .mapToLong(row -> ((Number) row[2]).longValue())
            .sum();

        List<CategoryRatioDTO> ratioList = results.stream()
            .map(row -> {
                long count = ((Number) row[2]).longValue();
                BigDecimal ratio = totalBorrows > 0
                    ? BigDecimal.valueOf(count)
                        .divide(BigDecimal.valueOf(totalBorrows), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
                return new CategoryRatioDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    count,
                    ratio
                );
            })
            .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, ratioList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return ratioList;
    }

    public List<CityActivityDTO> getCityActivityRank(StatisticsQueryDTO queryDTO) {
        String cacheKey = STATISTICS_CACHE_KEY + "city:" + buildCacheKey(queryDTO);
        @SuppressWarnings("unchecked")
        List<CityActivityDTO> cached = (List<CityActivityDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime startTime = getStartTime(queryDTO.getStartDate());
        LocalDateTime endTime = getEndTime(queryDTO.getEndDate());
        int topN = queryDTO.getTopN() != null ? queryDTO.getTopN() : 10;

        List<Object[]> results = borrowRecordRepository.countActivityByCity(startTime, endTime);

        List<CityActivityDTO> rankList = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, results.size()); i++) {
            Object[] row = results.get(i);
            rankList.add(new CityActivityDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                i + 1
            ));
        }

        redisTemplate.opsForValue().set(cacheKey, rankList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rankList;
    }

    public List<BookRankDTO> getHotBooksRank(StatisticsQueryDTO queryDTO) {
        String cacheKey = STATISTICS_CACHE_KEY + "hotbooks:" + buildCacheKey(queryDTO);
        @SuppressWarnings("unchecked")
        List<BookRankDTO> cached = (List<BookRankDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime startTime = getStartTime(queryDTO.getStartDate());
        LocalDateTime endTime = getEndTime(queryDTO.getEndDate());
        int topN = queryDTO.getTopN() != null ? queryDTO.getTopN() : 10;

        List<Object[]> results = borrowRecordRepository.findHotBooks(startTime, endTime);

        List<BookRankDTO> rankList = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, results.size()); i++) {
            Object[] row = results.get(i);
            rankList.add(new BookRankDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                ((Number) row[5]).longValue(),
                i + 1
            ));
        }

        redisTemplate.opsForValue().set(cacheKey, rankList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rankList;
    }

    public List<UserBorrowRankDTO> getUserBorrowRank(StatisticsQueryDTO queryDTO) {
        String cacheKey = STATISTICS_CACHE_KEY + "userrank:" + buildCacheKey(queryDTO);
        @SuppressWarnings("unchecked")
        List<UserBorrowRankDTO> cached = (List<UserBorrowRankDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime startTime = getStartTime(queryDTO.getStartDate());
        LocalDateTime endTime = getEndTime(queryDTO.getEndDate());
        int topN = queryDTO.getTopN() != null ? queryDTO.getTopN() : 10;

        List<Object[]> results = borrowRecordRepository.findActiveUsers(startTime, endTime);

        List<UserBorrowRankDTO> rankList = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, results.size()); i++) {
            Object[] row = results.get(i);
            rankList.add(new UserBorrowRankDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                i + 1
            ));
        }

        redisTemplate.opsForValue().set(cacheKey, rankList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rankList;
    }

    private LocalDateTime getStartTime(LocalDate date) {
        if (date == null) {
            return LocalDateTime.of(LocalDate.now().minusMonths(3), LocalTime.MIN);
        }
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    private LocalDateTime getEndTime(LocalDate date) {
        if (date == null) {
            return LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        }
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    private String buildCacheKey(StatisticsQueryDTO queryDTO) {
        StringBuilder sb = new StringBuilder();
        if (queryDTO.getStartDate() != null) {
            sb.append(queryDTO.getStartDate()).append(":");
        }
        if (queryDTO.getEndDate() != null) {
            sb.append(queryDTO.getEndDate()).append(":");
        }
        if (StringUtils.hasText(queryDTO.getDimension())) {
            sb.append(queryDTO.getDimension()).append(":");
        }
        if (queryDTO.getTopN() != null) {
            sb.append(queryDTO.getTopN());
        }
        return sb.toString();
    }
}
