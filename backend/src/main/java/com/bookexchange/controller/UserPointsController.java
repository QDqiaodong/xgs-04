package com.bookexchange.controller;

import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.PointsRecordQueryDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.dto.UserLevelDTO;
import com.bookexchange.dto.UserPointsInfoDTO;
import com.bookexchange.entity.PointsRecord;
import com.bookexchange.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class UserPointsController {

    private final UserPointsService userPointsService;

    @GetMapping("/user/{userId}")
    public Result<UserPointsInfoDTO> getUserPointsInfo(@PathVariable Long userId) {
        UserPointsInfoDTO info = userPointsService.getUserPointsInfo(userId);
        return info != null ? Result.success(info) : Result.error("用户不存在");
    }

    @PostMapping("/records")
    public Result<PageResult<PointsRecord>> getPointsRecords(@RequestBody PointsRecordQueryDTO queryDTO) {
        PageResult<PointsRecord> result = userPointsService.getPointsRecords(queryDTO);
        return Result.success(result);
    }

    @GetMapping("/levels")
    public Result<List<UserLevelDTO>> getAllLevels() {
        return Result.success(userPointsService.getAllLevels());
    }

    @GetMapping("/user/{userId}/max-borrow")
    public Result<Integer> getMaxBorrowCount(@PathVariable Long userId) {
        return Result.success(userPointsService.getMaxBorrowCount(userId));
    }
}
