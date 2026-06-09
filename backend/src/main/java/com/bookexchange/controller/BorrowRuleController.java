package com.bookexchange.controller;

import com.bookexchange.dto.BorrowRuleDTO;
import com.bookexchange.dto.Result;
import com.bookexchange.entity.BorrowRule;
import com.bookexchange.service.BorrowRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrow-rules")
@RequiredArgsConstructor
public class BorrowRuleController {

    private final BorrowRuleService borrowRuleService;

    @GetMapping
    public Result<BorrowRule> getBorrowRule() {
        return Result.success(borrowRuleService.getBorrowRule());
    }

    @PutMapping
    public Result<BorrowRule> updateBorrowRule(@RequestBody BorrowRuleDTO dto) {
        return Result.success(borrowRuleService.updateBorrowRule(dto));
    }
}
