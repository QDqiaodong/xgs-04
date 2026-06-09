package com.bookexchange.service;

import com.bookexchange.dto.BookBatchOperationDTO;
import com.bookexchange.dto.BookBatchResultDTO;
import com.bookexchange.dto.BookDTO;
import com.bookexchange.dto.BookQueryDTO;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BookTag;
import com.bookexchange.entity.Category;
import com.bookexchange.entity.City;
import com.bookexchange.entity.Tag;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BookTagRepository;
import com.bookexchange.repository.BorrowRecordRepository;
import com.bookexchange.repository.CategoryRepository;
import com.bookexchange.repository.CityRepository;
import com.bookexchange.repository.TagRepository;
import com.bookexchange.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private static final List<String> ACTIVE_BORROW_STATUSES = Arrays.asList("PENDING", "APPROVED", "BORROWING");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BORROWED_BOOK_KEY = "borrowed:book:";

    public Page<Book> queryBooks(BookQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
            queryDTO.getPageNum() - 1,
            queryDTO.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        List<Long> filteredBookIds = null;
        if (queryDTO.getTagIds() != null && !queryDTO.getTagIds().isEmpty()) {
            if (Boolean.TRUE.equals(queryDTO.getMatchAllTags())) {
                filteredBookIds = bookTagRepository.findBookIdsByAllTagIds(
                    queryDTO.getTagIds(), (long) queryDTO.getTagIds().size()
                );
            } else {
                filteredBookIds = bookTagRepository.findBookIdsByTagIds(queryDTO.getTagIds());
            }
            if (filteredBookIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        final List<Long> tagFilteredBookIds = filteredBookIds;

        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tagFilteredBookIds != null) {
                predicates.add(root.get("id").in(tagFilteredBookIds));
            }

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

        Page<Book> page = bookRepository.findAll(spec, pageable);
        page.getContent().forEach(this::enrichBookWithTags);
        return page;
    }

    public Book getBookById(Long id) {
        String key = BORROWED_BOOK_KEY + id;
        Book cachedBook = safeReadBook(key);
        if (cachedBook != null) {
            enrichBookWithTags(cachedBook);
            return cachedBook;
        }

        Book book = bookRepository.findById(id).orElse(null);
        if (book != null) {
            enrichBookWithTags(book);
            if (!book.getAvailable()) {
                try {
                    redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("Failed to cache book {}: {}", id, e.getMessage());
                }
            }
        }
        return book;
    }

    private Book safeReadBook(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof Book) {
                return (Book) value;
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

    private void enrichBookWithTags(Book book) {
        if (book != null && book.getTags() == null) {
            List<BookTag> bookTags = bookTagRepository.findByBookIdWithTag(book.getId());
            List<Tag> tags = bookTags.stream()
                .map(BookTag::getTag)
                .collect(Collectors.toList());
            book.setTags(tags);
        }
    }

    public List<Book> getBooksByOwnerId(Long ownerId) {
        List<Book> books = bookRepository.findByOwnerId(ownerId);
        books.forEach(this::enrichBookWithTags);
        return books;
    }

    @Transactional
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

        Book savedBook = bookRepository.save(book);

        if (bookDTO.getTagIds() != null && !bookDTO.getTagIds().isEmpty()) {
            saveBookTags(savedBook, bookDTO.getTagIds());
            enrichBookWithTags(savedBook);
        }

        return savedBook;
    }

    @Transactional
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

        if (bookDTO.getTagIds() != null) {
            bookTagRepository.deleteByBookId(id);
            if (!bookDTO.getTagIds().isEmpty()) {
                saveBookTags(book, bookDTO.getTagIds());
            }
        }

        Book savedBook = bookRepository.save(book);
        enrichBookWithTags(savedBook);
        return savedBook;
    }

    private void saveBookTags(Book book, List<Long> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        for (Tag tag : tags) {
            BookTag bookTag = new BookTag();
            bookTag.setBook(book);
            bookTag.setTag(tag);
            bookTagRepository.save(bookTag);
        }
    }

    @Transactional
    public boolean addTagsToBooks(List<Long> bookIds, List<Long> tagIds) {
        if (bookIds == null || bookIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
            return false;
        }
        List<Book> books = bookRepository.findAllById(bookIds);
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (books.isEmpty() || tags.isEmpty()) {
            return false;
        }
        for (Book book : books) {
            List<BookTag> existingBookTags = bookTagRepository.findByBookId(book.getId());
            List<Long> existingTagIds = existingBookTags.stream()
                .map(bt -> bt.getTag().getId())
                .collect(Collectors.toList());
            for (Tag tag : tags) {
                if (!existingTagIds.contains(tag.getId())) {
                    BookTag bookTag = new BookTag();
                    bookTag.setBook(book);
                    bookTag.setTag(tag);
                    bookTagRepository.save(bookTag);
                }
            }
        }
        return true;
    }

    @Transactional
    public boolean removeTagsFromBooks(List<Long> bookIds, List<Long> tagIds) {
        if (bookIds == null || bookIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
            return false;
        }
        for (Long bookId : bookIds) {
            bookTagRepository.deleteByBookIdAndTagIds(bookId, tagIds);
        }
        return true;
    }

    public void updateBookAvailability(Long bookId, boolean available) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null) {
            book.setAvailable(available);
            bookRepository.save(book);

            String key = BORROWED_BOOK_KEY + bookId;
            try {
                if (available) {
                    redisTemplate.delete(key);
                } else {
                    redisTemplate.opsForValue().set(key, book, 1, TimeUnit.HOURS);
                }
            } catch (Exception e) {
                log.warn("Failed to update cache for book {}: {}", bookId, e.getMessage());
            }
        }
    }

    @Transactional
    public void deleteBook(Long id) {
        bookTagRepository.deleteByBookId(id);
        bookRepository.deleteById(id);
        try {
            redisTemplate.delete(BORROWED_BOOK_KEY + id);
        } catch (Exception e) {
            log.warn("Failed to delete cache for book {}: {}", id, e.getMessage());
        }
    }

    @Transactional
    public BookBatchResultDTO batchOperate(BookBatchOperationDTO dto) {
        BookBatchResultDTO result = new BookBatchResultDTO();
        List<Long> bookIds = dto.getBookIds();
        if (bookIds == null || bookIds.isEmpty()) {
            result.setTotalCount(0);
            result.setSuccessCount(0);
            result.setFailCount(0);
            return result;
        }

        result.setTotalCount(bookIds.size());
        List<Long> successIds = new ArrayList<>();
        List<BookBatchResultDTO.BookBatchFailDetail> failDetails = new ArrayList<>();

        String operation = dto.getOperation();
        if ("UPDATE_CATEGORY".equals(operation)) {
            batchUpdateCategory(dto, bookIds, successIds, failDetails);
        } else if ("SET_CAN_BORROW".equals(operation)) {
            batchSetCanBorrow(dto, bookIds, successIds, failDetails);
        } else if ("SET_AVAILABLE".equals(operation)) {
            batchSetAvailable(dto, bookIds, successIds, failDetails);
        } else if ("DELETE".equals(operation)) {
            batchDelete(bookIds, successIds, failDetails);
        } else {
            for (Long bookId : bookIds) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "不支持的操作类型: " + operation));
            }
        }

        result.setSuccessIds(successIds);
        result.setFailDetails(failDetails);
        result.setSuccessCount(successIds.size());
        result.setFailCount(failDetails.size());
        return result;
    }

    private void batchUpdateCategory(BookBatchOperationDTO dto, List<Long> bookIds,
                                     List<Long> successIds, List<BookBatchResultDTO.BookBatchFailDetail> failDetails) {
        Long categoryId = dto.getCategoryId();
        if (categoryId == null) {
            for (Long bookId : bookIds) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "分类ID不能为空"));
            }
            return;
        }
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            for (Long bookId : bookIds) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "目标分类不存在"));
            }
            return;
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        for (Long bookId : bookIds) {
            Book book = books.stream().filter(b -> b.getId().equals(bookId)).findFirst().orElse(null);
            if (book == null) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "图书不存在"));
                continue;
            }
            try {
                book.setCategory(category);
                bookRepository.save(book);
                successIds.add(bookId);
            } catch (Exception e) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "更新失败: " + e.getMessage()));
            }
        }
    }

    private void batchSetCanBorrow(BookBatchOperationDTO dto, List<Long> bookIds,
                                   List<Long> successIds, List<BookBatchResultDTO.BookBatchFailDetail> failDetails) {
        Boolean canBorrow = dto.getCanBorrow();
        if (canBorrow == null) {
            for (Long bookId : bookIds) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "可借状态参数不能为空"));
            }
            return;
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        for (Long bookId : bookIds) {
            Book book = books.stream().filter(b -> b.getId().equals(bookId)).findFirst().orElse(null);
            if (book == null) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "图书不存在"));
                continue;
            }
            if (!canBorrow) {
                if (borrowRecordRepository.existsByBookIdAndStatusIn(bookId, ACTIVE_BORROW_STATUSES)) {
                    failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "存在活跃借阅记录，无法设置为不可借"));
                    continue;
                }
            }
            try {
                book.setCanBorrow(canBorrow);
                bookRepository.save(book);
                successIds.add(bookId);
            } catch (Exception e) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "更新失败: " + e.getMessage()));
            }
        }
    }

    private void batchSetAvailable(BookBatchOperationDTO dto, List<Long> bookIds,
                                   List<Long> successIds, List<BookBatchResultDTO.BookBatchFailDetail> failDetails) {
        Boolean available = dto.getAvailable();
        if (available == null) {
            for (Long bookId : bookIds) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "上下架状态参数不能为空"));
            }
            return;
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        for (Long bookId : bookIds) {
            Book book = books.stream().filter(b -> b.getId().equals(bookId)).findFirst().orElse(null);
            if (book == null) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "图书不存在"));
                continue;
            }
            if (!available) {
                if (borrowRecordRepository.existsByBookIdAndStatusIn(bookId, ACTIVE_BORROW_STATUSES)) {
                    failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "存在活跃借阅记录，无法下架"));
                    continue;
                }
            }
            try {
                updateBookAvailability(bookId, available);
                successIds.add(bookId);
            } catch (Exception e) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "更新失败: " + e.getMessage()));
            }
        }
    }

    private void batchDelete(List<Long> bookIds,
                             List<Long> successIds, List<BookBatchResultDTO.BookBatchFailDetail> failDetails) {
        List<Book> books = bookRepository.findAllById(bookIds);
        for (Long bookId : bookIds) {
            Book book = books.stream().filter(b -> b.getId().equals(bookId)).findFirst().orElse(null);
            if (book == null) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, null, "图书不存在"));
                continue;
            }
            if (borrowRecordRepository.existsByBookIdAndStatusIn(bookId, ACTIVE_BORROW_STATUSES)) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "存在活跃借阅记录，无法删除"));
                continue;
            }
            try {
                deleteBook(bookId);
                successIds.add(bookId);
            } catch (Exception e) {
                failDetails.add(new BookBatchResultDTO.BookBatchFailDetail(bookId, book.getTitle(), "删除失败: " + e.getMessage()));
            }
        }
    }
}
