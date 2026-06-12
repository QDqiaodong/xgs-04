package com.bookexchange.controller;

import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.ReservationDTO;
import com.bookexchange.dto.ReservationPositionDTO;
import com.bookexchange.dto.ReservationQueryDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.dto.ValidationResult;
import com.bookexchange.entity.Reservation;
import com.bookexchange.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/validate")
    public Result<ValidationResult> validateCreateReservation(@RequestBody ReservationDTO dto) {
        return Result.success(reservationService.validateCreateReservation(dto));
    }

    @PostMapping
    public Result<Reservation> createReservation(@RequestBody ReservationDTO dto) {
        ValidationResult validation = reservationService.validateCreateReservation(dto);
        if (!validation.isValid()) {
            return Result.error(validation.getMessage());
        }
        Reservation reservation = reservationService.createReservation(dto);
        if (reservation != null) {
            return Result.success(reservation);
        }
        validation = reservationService.validateCreateReservation(dto);
        if (!validation.isValid()) {
            return Result.error(validation.getMessage());
        }
        return Result.error("提交过于频繁，请稍后再试");
    }

    @GetMapping("/{id}")
    public Result<Reservation> getReservationById(@PathVariable Long id) {
        Reservation reservation = reservationService.getReservationById(id);
        return reservation != null ? Result.success(reservation) : Result.error("预约记录不存在");
    }

    @GetMapping("/user/{userId}")
    public Result<List<Reservation>> getReservationsByUserId(@PathVariable Long userId) {
        return Result.success(reservationService.getReservationsByUserId(userId));
    }

    @GetMapping("/book/{bookId}")
    public Result<List<Reservation>> getReservationsByBookId(@PathVariable Long bookId) {
        return Result.success(reservationService.getReservationsByBookId(bookId));
    }

    @PostMapping("/query")
    public Result<PageResult<Reservation>> queryReservations(@RequestBody ReservationQueryDTO queryDTO) {
        Page<Reservation> page = reservationService.queryReservations(queryDTO);
        return Result.success(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

    @GetMapping("/{id}/position")
    public Result<ReservationPositionDTO> getReservationPosition(@PathVariable Long id) {
        ReservationPositionDTO position = reservationService.getReservationPositionInfo(id);
        return position != null ? Result.success(position) : Result.error("预约记录不存在");
    }

    @DeleteMapping("/{id}/user/{userId}")
    public Result<Reservation> cancelReservation(@PathVariable Long id, @PathVariable Long userId) {
        Reservation reservation = reservationService.cancelReservation(id, userId);
        return reservation != null ? Result.success(reservation) : Result.error("取消预约失败");
    }

    @PutMapping("/{id}/confirm/user/{userId}")
    public Result<Boolean> confirmBorrowReservation(@PathVariable Long id, @PathVariable Long userId) {
        boolean success = reservationService.confirmBorrowReservation(id, userId);
        return success ? Result.success(true) : Result.error("确认借阅失败，预约状态无效或已超时");
    }

    @PostMapping("/process-expired")
    public Result<Void> processExpiredReservations() {
        reservationService.processExpiredReservations();
        return Result.success();
    }
}
