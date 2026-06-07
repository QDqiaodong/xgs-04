package com.bookexchange.controller;

import com.bookexchange.dto.BookDTO;
import com.bookexchange.dto.BookQueryDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.Book;
import com.bookexchange.service.BookService;
import com.bookexchange.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;

    @PostMapping("/query")
    public Result<Page<Book>> queryBooks(@RequestBody BookQueryDTO queryDTO) {
        Page<Book> page = bookService.queryBooks(queryDTO);
        page.getContent().forEach(this::enrichBookWithReviewStats);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book != null) {
            enrichBookWithReviewStats(book);
            return Result.success(book);
        }
        return Result.error("图书不存在");
    }

    @GetMapping("/owner/{ownerId}")
    public Result<List<Book>> getBooksByOwnerId(@PathVariable Long ownerId) {
        List<Book> books = bookService.getBooksByOwnerId(ownerId);
        books.forEach(this::enrichBookWithReviewStats);
        return Result.success(books);
    }

    @PostMapping
    public Result<Book> createBook(@RequestBody BookDTO bookDTO) {
        Book book = bookService.createBook(bookDTO);
        return book != null ? Result.success(book) : Result.error("创建图书失败");
    }

    @PutMapping("/{id}")
    public Result<Book> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        Book book = bookService.updateBook(id, bookDTO);
        return book != null ? Result.success(book) : Result.error("更新图书失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }

    private void enrichBookWithReviewStats(Book book) {
        book.setAverageRating(reviewService.getAverageRatingByBookId(book.getId()));
        book.setReviewCount(reviewService.getReviewCountByBookId(book.getId()));
    }
}
