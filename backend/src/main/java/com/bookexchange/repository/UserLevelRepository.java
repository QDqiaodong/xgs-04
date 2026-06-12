package com.bookexchange.repository;

import com.bookexchange.entity.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    List<UserLevel> findAllByOrderBySortOrderAsc();

    Optional<UserLevel> findByCode(String code);

    List<UserLevel> findByMinPointsLessThanEqualOrderByMinPointsDesc(Integer points);
}
