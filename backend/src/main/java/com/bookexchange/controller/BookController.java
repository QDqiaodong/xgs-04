package com.bookexchange.controller;

import com.bookexchange.dto.BookDTO;
import com.bookexchange.dto.BookQueryDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.Book;
import com.bookexchange.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/query")
    public Result<Page<Book>> queryBooks(@RequestBody BookQueryDTO queryDTO) {
        return Result.success(bookService.queryBooks(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        return book != null ? Result.success(book) : Result.error("图书不存在");
    }

    @GetMapping("/owner/{ownerId}")
    public Result<List<Book>> getBooksByOwnerId(@PathVariable Long ownerId) {
        return Result.success(bookService.getBooksByOwnerId(ownerId));
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
}
