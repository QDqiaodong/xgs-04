package com.bookexchange.service;

import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.PointsRecordQueryDTO;
import com.bookexchange.dto.UserLevelDTO;
import com.bookexchange.dto.UserPointsInfoDTO;
import com.bookexchange.entity.PointsRecord;
import com.bookexchange.entity.User;
import com.bookexchange.entity.UserLevel;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.PointsRecordRepository;
import com.bookexchange.repository.UserLevelRepository;
import com.bookexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPointsService {

    private final PointsRecordRepository pointsRecordRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    private static final List<String> ACTIVE_STATUSES = Arrays.asList("PENDING", "APPROVED", "BORROWING", "OVERDUE");

    private static final int POINTS_LEND_BOOK = 5;
    private static final int POINTS_RETURN_ON_TIME = 10;
    private static final int POINTS_OVERDUE_PER_DAY = 5;

    @Transactional(rollbackFor = Exception.class)
    public PointsRecord addPoints(Long userId, int points, String type, String source, String description, Long relatedId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        user.setPoints(user.getPoints() + points);
        String newLevelCode = calculateLevel(user.getPoints());
        user.setLevel(newLevelCode);
        userRepository.save(user);

        PointsRecord record = new PointsRecord();
        record.setUser(user);
        record.setPoints(points);
        record.setBalanceAfter(user.getPoints());
        record.setType(type);
        record.setSource(source);
        record.setDescription(description);
        record.setRelatedId(relatedId);
        return pointsRecordRepository.save(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public PointsRecord deductPoints(Long userId, int points, String source, String description, Long relatedId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        int newPoints = Math.max(0, user.getPoints() - points);
        user.setPoints(newPoints);
        String newLevelCode = calculateLevel(newPoints);
        user.setLevel(newLevelCode);
        userRepository.save(user);

        PointsRecord record = new PointsRecord();
        record.setUser(user);
        record.setPoints(-points);
        record.setBalanceAfter(newPoints);
        record.setType("DEDUCT");
        record.setSource(source);
        record.setDescription(description);
        record.setRelatedId(relatedId);
        return pointsRecordRepository.save(record);
    }

    public void awardLendPoints(Long ownerId, Long borrowRecordId) {
        addPoints(ownerId, POINTS_LEND_BOOK, "EARN", "LEND_BOOK",
                "出借图书成功，获得" + POINTS_LEND_BOOK + "积分", borrowRecordId);
    }

    public void awardReturnOnTimePoints(Long borrowerId, Long borrowRecordId) {
        addPoints(borrowerId, POINTS_RETURN_ON_TIME, "EARN", "RETURN_ON_TIME",
                "按时归还图书，获得" + POINTS_RETURN_ON_TIME + "积分", borrowRecordId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deductOverduePoints(Long borrowerId, int overdueDays, Long borrowRecordId) {
        int deductPoints = overdueDays * POINTS_OVERDUE_PER_DAY;
        deductPoints(borrowerId, deductPoints, "OVERDUE_RETURN",
                "逾期归还" + overdueDays + "天，扣除" + deductPoints + "积分", borrowRecordId);
    }

    private String calculateLevel(Integer points) {
        List<UserLevel> levels = userLevelRepository.findAllByOrderBySortOrderAsc();
        if (levels.isEmpty()) {
            return "BRONZE";
        }

        String levelCode = levels.get(0).getCode();
        for (UserLevel level : levels) {
            if (points >= level.getMinPoints()) {
                levelCode = level.getCode();
            } else {
                break;
            }
        }
        return levelCode;
    }

    public UserLevel getUserLevelByCode(String code) {
        return userLevelRepository.findByCode(code).orElse(null);
    }

    public int getMaxBorrowCount(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 3;
        }
        UserLevel level = userLevelRepository.findByCode(user.getLevel()).orElse(null);
        if (level == null) {
            return 3;
        }
        return level.getMaxBorrowCount();
    }

    public UserPointsInfoDTO getUserPointsInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        UserPointsInfoDTO dto = new UserPointsInfoDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setTotalPoints(user.getPoints());

        UserLevel currentLevel = userLevelRepository.findByCode(user.getLevel()).orElse(null);
        if (currentLevel != null) {
            dto.setLevelCode(currentLevel.getCode());
            dto.setLevelName(currentLevel.getName());
            dto.setMaxBorrowCount(currentLevel.getMaxBorrowCount());
        } else {
            dto.setLevelCode("BRONZE");
            dto.setLevelName("青铜");
            dto.setMaxBorrowCount(3);
        }

        List<UserLevel> allLevels = userLevelRepository.findAllByOrderBySortOrderAsc();
        UserLevel nextLevel = null;
        for (int i = 0; i < allLevels.size(); i++) {
            if (allLevels.get(i).getCode().equals(user.getLevel()) && i + 1 < allLevels.size()) {
                nextLevel = allLevels.get(i + 1);
                break;
            }
        }
        if (nextLevel != null) {
            dto.setNextLevelPoints(nextLevel.getMinPoints());
            dto.setNextLevelName(nextLevel.getName());
        }

        long activeBorrows = borrowRecordRepository.countByBorrowerIdAndStatusIn(userId, ACTIVE_STATUSES);
        dto.setCurrentActiveBorrows((int) activeBorrows);

        return dto;
    }

    public PageResult<PointsRecord> getPointsRecords(PointsRecordQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageNum() - 1,
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<PointsRecord> page;
        if (StringUtils.hasText(queryDTO.getType())) {
            page = pointsRecordRepository.findByUserIdAndTypeOrderByCreateTimeDesc(
                    queryDTO.getUserId(), queryDTO.getType(), pageable);
        } else {
            page = pointsRecordRepository.findByUserIdOrderByCreateTimeDesc(
                    queryDTO.getUserId(), pageable);
        }

        return new PageResult<>(page.getContent(), page.getTotalElements());
    }

    public List<UserLevelDTO> getAllLevels() {
        List<UserLevel> levels = userLevelRepository.findAllByOrderBySortOrderAsc();
        return levels.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private UserLevelDTO convertToDTO(UserLevel level) {
        UserLevelDTO dto = new UserLevelDTO();
        dto.setId(level.getId());
        dto.setCode(level.getCode());
        dto.setName(level.getName());
        dto.setMinPoints(level.getMinPoints());
        dto.setMaxBorrowCount(level.getMaxBorrowCount());
        dto.setSortOrder(level.getSortOrder());
        dto.setIcon(level.getIcon());
        dto.setDescription(level.getDescription());
        return dto;
    }
}
