package com.bookexchange.service;

import com.bookexchange.dto.BookDTO;
import com.bookexchange.dto.BookQueryDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.Category;
import com.bookexchange.entity.City;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.CategoryRepository;
import com.bookexchange.repository.CityRepository;
import com.bookexchange.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BORROWED_BOOK_KEY = "borrowed:book:";

    public Page<Book> queryBooks(BookQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getCityId() != null) {
                predicates.add(cb.equal(root.get("city").get("id"), queryDTO.getCityId()));
            }
            if (queryDTO.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), queryDTO.getCategoryId()));
            }
            if (queryDTO.getAvailable() != null) {
                predicates.add(cb.equal(root.get("available"), queryDTO.getAvailable()));
            }
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                String keyword = "%" + queryDTO.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), keyword),
                    cb.like(root.get("author"), keyword)
                ));
            }

            predicates.add(cb.equal(root.get("canBorrow"), true));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookRepository.findAll(spec, pageable);
    }

    public Book getBookById(Long id) {
        String key = BORROWED_BOOK_KEY + id;
        Book cachedBook = (Book) redisTemplate.opsForValue().get(key);
        if (cachedBook != null) {
            return cachedBook;
        }

        Book book = bookRepository.findById(id).orElse(null);
        if (book != null && !book.getAvailable()) {
            redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
        }
        return book;
    }

    public List<Book> getBooksByOwnerId(Long ownerId) {
        return bookRepository.findByOwnerId(ownerId);
    }

    public Book createBook(BookDTO bookDTO) {
        Category category = categoryRepository.findById(bookDTO.getCategoryId()).orElse(null);
        City city = cityRepository.findById(bookDTO.getCityId()).orElse(null);
        User owner = userRepository.findById(bookDTO.getOwnerId()).orElse(null);

        if (category == null || city == null || owner == null) {
            return null;
        }

        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setIsbn(bookDTO.getIsbn());
        book.setCategory(category);
        book.setConditionLevel(bookDTO.getConditionLevel());
        book.setDescription(bookDTO.getDescription());
        book.setCanBorrow(bookDTO.getCanBorrow() != null ? bookDTO.getCanBorrow() : true);
        book.setAvailable(true);
        book.setOwner(owner);
        book.setCity(city);

        return bookRepository.save(book);
    }

    public Book updateBook(Long id, BookDTO bookDTO) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return null;
        }

        if (bookDTO.getTitle() != null) {
            book.setTitle(bookDTO.getTitle());
        }
        if (bookDTO.getAuthor() != null) {
            book.setAuthor(bookDTO.getAuthor());
        }
        if (bookDTO.getIsbn() != null) {
            book.setIsbn(bookDTO.getIsbn());
        }
        if (bookDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookDTO.getCategoryId()).orElse(null);
            if (category != null) {
                book.setCategory(category);
            }
        }
        if (bookDTO.getConditionLevel() != null) {
            book.setConditionLevel(bookDTO.getConditionLevel());
        }
        if (bookDTO.getDescription() != null) {
            book.setDescription(bookDTO.getDescription());
        }
        if (bookDTO.getCanBorrow() != null) {
            book.setCanBorrow(bookDTO.getCanBorrow());
        }

        return bookRepository.save(book);
    }

    public void updateBookAvailability(Long bookId, boolean available) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null) {
            book.setAvailable(available);
            bookRepository.save(book);

            String key = BORROWED_BOOK_KEY + bookId;
            if (available) {
                redisTemplate.delete(key);
            } else {
                redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
            }
        }
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
        redisTemplate.delete(BORROWED_BOOK_KEY + id);
    }
}
