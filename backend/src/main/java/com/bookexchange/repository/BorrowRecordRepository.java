package com.bookexchange.repository;

import com.bookexchange.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Query("SELECT FUNCTION('DATE_FORMAT', br.createTime, '%Y-%m-%d') as period, " +
           "SUM(CASE WHEN br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount, " +
           "SUM(CASE WHEN br.status = 'RETURNED' AND br.returnTime BETWEEN :startTime AND :endTime THEN 1 ELSE 0 END) as returnCount, " +
           "COUNT(br) as totalCount " +
           "FROM BorrowRecord br " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', br.createTime, '%Y-%m-%d') " +
           "ORDER BY period")
    List<Object[]> countBorrowsByDay(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT FUNCTION('DATE_FORMAT', br.createTime, '%Y-%u') as period, " +
           "SUM(CASE WHEN br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount, " +
           "SUM(CASE WHEN br.status = 'RETURNED' AND br.returnTime BETWEEN :startTime AND :endTime THEN 1 ELSE 0 END) as returnCount, " +
           "COUNT(br) as totalCount " +
           "FROM BorrowRecord br " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', br.createTime, '%Y-%u') " +
           "ORDER BY period")
    List<Object[]> countBorrowsByWeek(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT FUNCTION('DATE_FORMAT', br.createTime, '%Y-%m') as period, " +
           "SUM(CASE WHEN br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount, " +
           "SUM(CASE WHEN br.status = 'RETURNED' AND br.returnTime BETWEEN :startTime AND :endTime THEN 1 ELSE 0 END) as returnCount, " +
           "COUNT(br) as totalCount " +
           "FROM BorrowRecord br " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', br.createTime, '%Y-%m') " +
           "ORDER BY period")
    List<Object[]> countBorrowsByMonth(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT c.id, c.name, " +
           "SUM(CASE WHEN br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount " +
           "FROM BorrowRecord br " +
           "JOIN br.book b " +
           "JOIN b.category c " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.id, c.name " +
           "ORDER BY borrowCount DESC")
    List<Object[]> countBorrowsByCategory(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT city.id, city.province, city.cityName, " +
           "SUM(CASE WHEN br.borrower.city.id = city.id AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount, " +
           "SUM(CASE WHEN br.owner.city.id = city.id AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as lendCount, " +
           "SUM(CASE WHEN (br.borrower.city.id = city.id OR br.owner.city.id = city.id) AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as totalActivity " +
           "FROM BorrowRecord br " +
           "JOIN br.borrower borrower " +
           "JOIN br.owner owner " +
           "JOIN City city ON (borrower.city.id = city.id OR owner.city.id = city.id) " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY city.id, city.province, city.cityName " +
           "ORDER BY totalActivity DESC")
    List<Object[]> countActivityByCity(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b.id, b.title, b.author, b.isbn, c.name as categoryName, " +
           "SUM(CASE WHEN br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount " +
           "FROM BorrowRecord br " +
           "JOIN br.book b " +
           "JOIN b.category c " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY b.id, b.title, b.author, b.isbn, c.name " +
           "ORDER BY borrowCount DESC")
    List<Object[]> findHotBooks(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT u.id, u.username, u.nickname, u.avatar, " +
           "SUM(CASE WHEN br.borrower.id = u.id AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as borrowCount, " +
           "SUM(CASE WHEN br.owner.id = u.id AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as lendCount, " +
           "SUM(CASE WHEN (br.borrower.id = u.id OR br.owner.id = u.id) AND br.status IN ('APPROVED', 'BORROWING', 'RETURNED', 'OVERDUE') THEN 1 ELSE 0 END) as totalActivity " +
           "FROM BorrowRecord br " +
           "JOIN br.borrower borrower " +
           "JOIN br.owner owner " +
           "JOIN User u ON (borrower.id = u.id OR owner.id = u.id) " +
           "WHERE br.createTime BETWEEN :startTime AND :endTime " +
           "GROUP BY u.id, u.username, u.nickname, u.avatar " +
           "ORDER BY totalActivity DESC")
    List<Object[]> findActiveUsers(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
