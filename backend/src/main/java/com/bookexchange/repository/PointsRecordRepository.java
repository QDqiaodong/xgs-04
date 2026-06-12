package com.bookexchange.repository;

import com.bookexchange.entity.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long>, JpaSpecificationExecutor<PointsRecord> {

    Page<PointsRecord> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<PointsRecord> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type, Pageable pageable);
}
