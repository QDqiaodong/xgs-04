package com.bookexchange.controller;

import com.bookexchange.dto.BorrowRecordDTO;
import com.bookexchange.dto.BorrowRecordQueryDTO;
import com.bookexchange.dto.OverdueQueryDTO;
import com.bookexchange.dto.PageResult;
import com.bookexchange.dto.Result;
import com.bookexchange.dto.ValidationResult;
import com.bookexchange.entity.BorrowRecord;
import com.bookexchange.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrow-records")
@RequiredArgsConstructor
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;

    @PostMapping("/query")
    public Result<PageResult<BorrowRecord>> queryBorrowRecords(@RequestBody BorrowRecordQueryDTO queryDTO) {
        Page<BorrowRecord> page = borrowRecordService.queryBorrowRecords(queryDTO);
        return Result.success(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

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

    @PostMapping("/validate-create")
    public Result<ValidationResult> validateCreateBorrowRecord(@RequestBody BorrowRecordDTO dto) {
        return Result.success(borrowRecordService.validateCreateBorrowRecord(dto));
    }

    @PostMapping
    public Result<BorrowRecord> createBorrowRecord(@RequestBody BorrowRecordDTO dto) {
        ValidationResult validation = borrowRecordService.validateCreateBorrowRecord(dto);
        if (!validation.isValid()) {
            return Result.error(validation.getMessage());
        }
        BorrowRecord record = borrowRecordService.createBorrowRecord(dto);
        if (record != null) {
            return Result.success(record);
        }
        validation = borrowRecordService.validateCreateBorrowRecord(dto);
        if (!validation.isValid()) {
            return Result.error(validation.getMessage());
        }
        return Result.error("提交过于频繁，请稍后再试");
    }

    @GetMapping("/{id}/validate-approve")
    public Result<ValidationResult> validateApproveBorrowRecord(@PathVariable Long id) {
        return Result.success(borrowRecordService.validateApproveBorrowRecord(id));
    }

    @PutMapping("/{id}/approve")
    public Result<BorrowRecord> approveBorrowRecord(@PathVariable Long id) {
        ValidationResult validation = borrowRecordService.validateApproveBorrowRecord(id);
        if (!validation.isValid()) {
            return Result.error(validation.getMessage());
        }
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

    @PostMapping("/overdue/identify")
    public Result<Integer> identifyOverdueRecords() {
        int count = borrowRecordService.identifyOverdueRecords();
        return Result.success(count);
    }

    @PostMapping("/overdue/query")
    public Result<PageResult<BorrowRecord>> queryOverdueRecords(@RequestBody OverdueQueryDTO queryDTO) {
        Page<BorrowRecord> page = borrowRecordService.queryOverdueRecords(queryDTO);
        return Result.success(new PageResult<>(page.getContent(), page.getTotalElements()));
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
