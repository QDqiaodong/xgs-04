package com.bookexchange.controller;

import com.bookexchange.dto.BookBatchOperationDTO;
import com.bookexchange.dto.BookBatchResultDTO;
import com.bookexchange.dto.BookDTO;
import com.bookexchange.dto.BookQueryDTO;
import com.bookexchange.dto.BookTagDTO;
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

    @PostMapping("/tags/add")
    public Result<Void> addTagsToBooks(@RequestBody BookTagDTO bookTagDTO) {
        boolean success = bookService.addTagsToBooks(bookTagDTO.getBookIds(), bookTagDTO.getTagIds());
        return success ? Result.success() : Result.error("批量打标失败");
    }

    @PostMapping("/tags/remove")
    public Result<Void> removeTagsFromBooks(@RequestBody BookTagDTO bookTagDTO) {
        boolean success = bookService.removeTagsFromBooks(bookTagDTO.getBookIds(), bookTagDTO.getTagIds());
        return success ? Result.success() : Result.error("批量取消标签失败");
    }

    @PostMapping("/batch")
    public Result<BookBatchResultDTO> batchOperate(@RequestBody BookBatchOperationDTO dto) {
        if (dto.getOperation() == null || dto.getOperation().trim().isEmpty()) {
            return Result.error("操作类型不能为空");
        }
        if (dto.getBookIds() == null || dto.getBookIds().isEmpty()) {
            return Result.error("图书ID列表不能为空");
        }
        BookBatchResultDTO result = bookService.batchOperate(dto);
        return Result.success(result);
    }

    private void enrichBookWithReviewStats(Book book) {
        book.setAverageRating(reviewService.getAverageRatingByBookId(book.getId()));
        book.setReviewCount(reviewService.getReviewCountByBookId(book.getId()));
    }
}
