package com.bookexchange.service;

import com.bookexchange.dto.BorrowRuleDTO;
import com.bookexchange.entity.BorrowRule;
import com.bookexchange.repository.BorrowRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BorrowRuleService {

    private final BorrowRuleRepository borrowRuleRepository;

    public BorrowRule getBorrowRule() {
        BorrowRule rule = borrowRuleRepository.findFirstByOrderByIdAsc();
        if (rule == null) {
            rule = createDefaultRule();
        }
        return rule;
    }

    public BorrowRule updateBorrowRule(BorrowRuleDTO dto) {
        BorrowRule rule = borrowRuleRepository.findFirstByOrderByIdAsc();
        if (rule == null) {
            rule = new BorrowRule();
        }

        if (dto.getMaxBorrowCount() != null && dto.getMaxBorrowCount() > 0) {
            rule.setMaxBorrowCount(dto.getMaxBorrowCount());
        }
        if (dto.getMaxBorrowDays() != null && dto.getMaxBorrowDays() > 0) {
            rule.setMaxBorrowDays(dto.getMaxBorrowDays());
        }
        if (dto.getReservationHours() != null && dto.getReservationHours() > 0) {
            rule.setReservationHours(dto.getReservationHours());
        }
        if (dto.getAllowRenew() != null) {
            rule.setAllowRenew(dto.getAllowRenew());
        }
        if (dto.getMaxRenewCount() != null && dto.getMaxRenewCount() >= 0) {
            rule.setMaxRenewCount(dto.getMaxRenewCount());
        }
        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription());
        }

        return borrowRuleRepository.save(rule);
    }

    private BorrowRule createDefaultRule() {
        BorrowRule rule = new BorrowRule();
        rule.setMaxBorrowCount(5);
        rule.setMaxBorrowDays(30);
        rule.setReservationHours(48);
        rule.setAllowRenew(true);
        rule.setMaxRenewCount(2);
        rule.setDescription("系统默认借阅规则");
        return borrowRuleRepository.save(rule);
    }
}
