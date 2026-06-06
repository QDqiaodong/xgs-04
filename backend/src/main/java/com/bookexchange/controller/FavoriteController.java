package com.bookexchange.controller;

import com.bookexchange.dto.Result;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.Favorite;
import com.bookexchange.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/user/{userId}")
    public Result<List<Book>> getFavorites(@PathVariable Long userId) {
        return Result.success(favoriteService.getFavoriteBooks(userId));
    }

    @GetMapping("/user/{userId}/check/{bookId}")
    public Result<Map<String, Boolean>> checkFavorite(
            @PathVariable Long userId,
            @PathVariable Long bookId
    ) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favoriteService.isFavorited(userId, bookId));
        return Result.success(result);
    }

    @GetMapping("/user/{userId}/count")
    public Result<Map<String, Long>> getFavoriteCount(@PathVariable Long userId) {
        Map<String, Long> result = new HashMap<>();
        result.put("count", favoriteService.getFavoriteCount(userId));
        return Result.success(result);
    }

    @PostMapping
    public Result<Favorite> addFavorite(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long bookId = params.get("bookId");
        if (userId == null || bookId == null) {
            return Result.error("参数错误");
        }
        Favorite favorite = favoriteService.addFavorite(userId, bookId);
        return favorite != null ? Result.success(favorite) : Result.error("收藏失败或已收藏");
    }

    @DeleteMapping("/user/{userId}/book/{bookId}")
    public Result<Void> removeFavorite(
            @PathVariable Long userId,
            @PathVariable Long bookId
    ) {
        boolean removed = favoriteService.removeFavorite(userId, bookId);
        return removed ? Result.success() : Result.error("取消收藏失败");
    }

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Long bookId = params.get("bookId");
        if (userId == null || bookId == null) {
            return Result.error("参数错误");
        }
        boolean favorited = favoriteService.toggleFavorite(userId, bookId);
        long count = favoriteService.getFavoriteCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        result.put("count", count);
        return Result.success(result);
    }
}
