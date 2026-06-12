package com.bookexchange.repository;

import com.bookexchange.entity.BrowseFootprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrowseFootprintRepository extends JpaRepository<BrowseFootprint, Long> {

    Page<BrowseFootprint> findByUserId(Long userId, Pageable pageable);

    Optional<BrowseFootprint> findByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
