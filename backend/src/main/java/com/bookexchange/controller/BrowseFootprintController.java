package com.bookexchange.controller;

import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BrowseFootprint;
import com.bookexchange.service.BrowseFootprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/browse-footprints")
@RequiredArgsConstructor
public class BrowseFootprintController {

    private final BrowseFootprintService browseFootprintService;

    @PostMapping
    public Result<BrowseFootprint> recordFootprint(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long bookId = params.get("bookId");
        if (userId == null || bookId == null) {
            return Result.error("参数错误");
        }
        BrowseFootprint footprint = browseFootprintService.recordFootprint(userId, bookId);
        return footprint != null ? Result.success(footprint) : Result.error("记录浏览足迹失败");
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<Book>> getFootprints(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Book> result = browseFootprintService.getFootprintBooks(userId, pageNum, pageSize);
        return Result.success(result);
    }

    @DeleteMapping("/user/{userId}/book/{bookId}")
    public Result<Void> deleteFootprint(
            @PathVariable Long userId,
            @PathVariable Long bookId) {
        boolean deleted = browseFootprintService.deleteFootprint(userId, bookId);
        return deleted ? Result.success() : Result.error("删除足迹失败，记录不存在");
    }

    @DeleteMapping("/user/{userId}")
    public Result<Void> clearFootprints(@PathVariable Long userId) {
        browseFootprintService.clearFootprints(userId);
        return Result.success();
    }
}
