package com.bookexchange.controller;

import com.bookexchange.dto.NotificationQueryDTO;
import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.Notification;
import com.bookexchange.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/query")
    public Result<PageResult<Notification>> queryNotifications(@RequestBody NotificationQueryDTO queryDTO) {
        Page<Notification> page = notificationService.queryNotifications(queryDTO);
        return Result.success(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = notificationService.markAsRead(id, userId);
        return success ? Result.success() : Result.error("操作失败");
    }

    @PutMapping("/read-all")
    public Result<Map<String, Object>> markAllAsRead(@RequestParam Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return Result.success(Map.of("updatedCount", count));
    }
}
