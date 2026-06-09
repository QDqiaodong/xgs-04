package com.bookexchange.repository;

import com.bookexchange.entity.BorrowRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowRuleRepository extends JpaRepository<BorrowRule, Long> {

    BorrowRule findFirstByOrderByIdAsc();
}
