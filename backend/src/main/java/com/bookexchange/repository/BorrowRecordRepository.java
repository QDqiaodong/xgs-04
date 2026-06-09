package com.bookexchange.repository;

import com.bookexchange.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long>, JpaSpecificationExecutor<BorrowRecord> {

    List<BorrowRecord> findByBorrowerIdOrderByCreateTimeDesc(Long borrowerId);

    List<BorrowRecord> findByOwnerIdOrderByCreateTimeDesc(Long ownerId);

    List<BorrowRecord> findByBookId(Long bookId);

    List<BorrowRecord> findByStatus(String status);

    List<BorrowRecord> findByBookIdAndStatusIn(Long bookId, List<String> statuses);

    boolean existsByBookIdAndStatusIn(Long bookId, List<String> statuses);

    long countByBorrowerIdAndStatusIn(Long borrowerId, List<String> statuses);
}
