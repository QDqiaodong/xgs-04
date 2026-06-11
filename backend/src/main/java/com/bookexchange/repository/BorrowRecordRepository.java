package com.bookexchange.repository;

import com.bookexchange.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    boolean existsByBookIdAndBorrowerIdAndStatusIn(Long bookId, Long borrowerId, List<String> statuses);

    long countByBookIdAndStatusIn(Long bookId, List<String> statuses);

    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.endDate < :date")
    List<BorrowRecord> findOverdueNotMarked(@Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE BorrowRecord br SET br.status = 'OVERDUE', br.overdueDays = :days WHERE br.id = :id AND br.status = 'BORROWING'")
    int markAsOverdue(@Param("id") Long id, @Param("days") int days);
}
