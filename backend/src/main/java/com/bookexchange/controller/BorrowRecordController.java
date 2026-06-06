package com.bookexchange.controller;

import com.bookexchange.dto.BorrowRecordDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrow-records")
@RequiredArgsConstructor
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;

    @GetMapping("/borrower/{borrowerId}")
    public Result<List<BorrowRecord>> getBorrowRecordsByBorrowerId(@PathVariable Long borrowerId) {
        return Result.success(borrowRecordService.getBorrowRecordsByBorrowerId(borrowerId));
    }

    @GetMapping("/owner/{ownerId}")
    public Result<List<BorrowRecord>> getBorrowRecordsByOwnerId(@PathVariable Long ownerId) {
        return Result.success(borrowRecordService.getBorrowRecordsByOwnerId(ownerId));
    }

    @GetMapping("/{id}")
    public Result<BorrowRecord> getBorrowRecordById(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.getBorrowRecordById(id);
        return record != null ? Result.success(record) : Result.error("借阅记录不存在");
    }

    @PostMapping
    public Result<BorrowRecord> createBorrowRecord(@RequestBody BorrowRecordDTO dto) {
        BorrowRecord record = borrowRecordService.createBorrowRecord(dto);
        return record != null ? Result.success(record) : Result.error("创建借阅申请失败");
    }

    @PutMapping("/{id}/approve")
    public Result<BorrowRecord> approveBorrowRecord(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.approveBorrowRecord(id);
        return record != null ? Result.success(record) : Result.error("审核失败");
    }

    @PutMapping("/{id}/reject")
    public Result<BorrowRecord> rejectBorrowRecord(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.rejectBorrowRecord(id);
        return record != null ? Result.success(record) : Result.error("拒绝失败");
    }

    @PutMapping("/{id}/confirm-borrow")
    public Result<BorrowRecord> confirmBorrow(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.confirmBorrow(id);
        return record != null ? Result.success(record) : Result.error("确认借出失败");
    }

    @PutMapping("/{id}/confirm-return")
    public Result<BorrowRecord> confirmReturn(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.confirmReturn(id);
        return record != null ? Result.success(record) : Result.error("确认归还失败");
    }

    @PostMapping("/filter/{userId}")
    public Result<Void> saveUserFilterConditions(@PathVariable Long userId, @RequestBody Object filterConditions) {
        borrowRecordService.saveUserFilterConditions(userId, filterConditions);
        return Result.success();
    }

    @GetMapping("/filter/{userId}")
    public Result<Object> getUserFilterConditions(@PathVariable Long userId) {
        return Result.success(borrowRecordService.getUserFilterConditions(userId));
    }
}
