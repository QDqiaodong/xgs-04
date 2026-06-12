package com.bookexchange.repository;

import com.bookexchange.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByBookIdAndStatusOrderByCreateTimeAsc(Long bookId, String status);

    List<Reservation> findByBookIdAndStatusInOrderByCreateTimeAsc(Long bookId, List<String> statuses);

    List<Reservation> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreateTimeAsc(Long bookId, String status);

    boolean existsByBookIdAndUserIdAndStatusIn(Long bookId, Long userId, List<String> statuses);

    long countByBookIdAndStatusIn(Long bookId, List<String> statuses);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.book.id = :bookId AND r.status IN ('WAITING', 'NOTIFIED') AND r.createTime < (SELECT r2.createTime FROM Reservation r2 WHERE r2.id = :reservationId)")
    long countPositionInQueue(@Param("bookId") Long bookId, @Param("reservationId") Long reservationId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'NOTIFIED' AND r.expireTime < :now")
    List<Reservation> findExpiredNotifiedReservations(@Param("now") LocalDateTime now);

    List<Reservation> findByBookIdOrderByCreateTimeAsc(Long bookId);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
