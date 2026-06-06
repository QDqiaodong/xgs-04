package com.bookexchange.service;

import com.bookexchange.entity.Book;
import com.bookexchange.entity.Favorite;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.FavoriteRepository;
import com.bookexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String FAVORITE_KEY_PREFIX = "favorite:user:";
    private static final String FAVORITE_COUNT_KEY_PREFIX = "favorite:count:";
    private static final long CACHE_EXPIRE_HOURS = 1;

    public List<Book> getFavoriteBooks(Long userId) {
        String key = FAVORITE_KEY_PREFIX + userId;
        List<Book> cachedBooks = safeReadBookList(key);
        if (cachedBooks != null) {
            return cachedBooks;
        }

        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        List<Book> books = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Book book = bookRepository.findById(favorite.getBook().getId()).orElse(null);
            if (book != null) {
                books.add(book);
            }
        }

        try {
            redisTemplate.opsForValue().set(key, books, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache favorite books for user {}: {}", userId, e.getMessage());
        }
        return books;
    }

    public boolean isFavorited(Long userId, Long bookId) {
        return favoriteRepository.existsByUserIdAndBookId(userId, bookId);
    }

    public long getFavoriteCount(Long userId) {
        String key = FAVORITE_COUNT_KEY_PREFIX + userId;
        Long cachedCount = safeReadLong(key);
        if (cachedCount != null) {
            return cachedCount;
        }

        long count = favoriteRepository.countByUserId(userId);
        try {
            redisTemplate.opsForValue().set(key, count, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache favorite count for user {}: {}", userId, e.getMessage());
        }
        return count;
    }

    private Long safeReadLong(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value != null) {
                log.warn("Unexpected cache value type for key {}: {}", key, value.getClass().getName());
            }
        } catch (Exception e) {
            log.warn("Failed to read cache for key {}: {}", key, e.getMessage());
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Book> safeReadBookList(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof List) {
                return (List<Book>) value;
            }
            log.warn("Unexpected cache value type for key {}: {}", key, value.getClass().getName());
        } catch (Exception e) {
            log.warn("Failed to read cache for key {}: {}", key, e.getMessage());
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Transactional
    public Favorite addFavorite(Long userId, Long bookId) {
        if (favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        Book book = bookRepository.findById(bookId).orElse(null);
        if (user == null || book == null) {
            return null;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);

        Favorite saved = favoriteRepository.save(favorite);
        evictCache(userId);
        return saved;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long bookId) {
        if (!favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            return false;
        }
        favoriteRepository.deleteByUserIdAndBookId(userId, bookId);
        evictCache(userId);
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId) {
        if (favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            favoriteRepository.deleteByUserIdAndBookId(userId, bookId);
            evictCache(userId);
            return false;
        } else {
            User user = userRepository.findById(userId).orElse(null);
            Book book = bookRepository.findById(bookId).orElse(null);
            if (user == null || book == null) {
                return false;
            }
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setBook(book);
            favoriteRepository.save(favorite);
            evictCache(userId);
            return true;
        }
    }

    private void evictCache(Long userId) {
        try {
            redisTemplate.delete(FAVORITE_KEY_PREFIX + userId);
            redisTemplate.delete(FAVORITE_COUNT_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to evict cache for user {}: {}", userId, e.getMessage());
        }
    }
}
