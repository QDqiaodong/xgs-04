package com.bookexchange.service;

import com.bookexchange.dto.PageResult;
import com.bookexchange.entity.Book;
import com.bookexchange.entity.BrowseFootprint;
import com.bookexchange.entity.User;
import com.bookexchange.repository.BookRepository;
import com.bookexchange.repository.BrowseFootprintRepository;
import com.bookexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrowseFootprintService {

    private static final Logger log = LoggerFactory.getLogger(BrowseFootprintService.class);

    private final BrowseFootprintRepository browseFootprintRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public BrowseFootprint recordFootprint(Long userId, Long bookId) {
        User user = userRepository.findById(userId).orElse(null);
        Book book = bookRepository.findById(bookId).orElse(null);
        if (user == null || book == null) {
            return null;
        }

        BrowseFootprint footprint = browseFootprintRepository
                .findByUserIdAndBookId(userId, bookId)
                .orElse(null);

        if (footprint != null) {
            footprint.setBrowseTime(LocalDateTime.now());
            return browseFootprintRepository.save(footprint);
        }

        footprint = new BrowseFootprint();
        footprint.setUser(user);
        footprint.setBook(book);
        footprint.setBrowseTime(LocalDateTime.now());
        return browseFootprintRepository.save(footprint);
    }

    public PageResult<Book> getFootprintBooks(Long userId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "browseTime"));

        Page<BrowseFootprint> footprintPage = browseFootprintRepository.findByUserId(userId, pageRequest);

        List<Book> books = new ArrayList<>();
        for (BrowseFootprint footprint : footprintPage.getContent()) {
            Book book = bookRepository.findById(footprint.getBook().getId()).orElse(null);
            if (book != null) {
                books.add(book);
            }
        }

        PageResult<Book> result = new PageResult<>();
        result.setList(books);
        result.setTotal(footprintPage.getTotalElements());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalPages(footprintPage.getTotalPages());
        return result;
    }

    @Transactional
    public boolean deleteFootprint(Long userId, Long bookId) {
        if (!browseFootprintRepository.findByUserIdAndBookId(userId, bookId).isPresent()) {
            return false;
        }
        browseFootprintRepository.deleteByUserIdAndBookId(userId, bookId);
        return true;
    }

    @Transactional
    public void clearFootprints(Long userId) {
        browseFootprintRepository.deleteByUserId(userId);
    }
}
