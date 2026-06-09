package com.bookexchange.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrow_rule")
public class BorrowRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_borrow_count", nullable = false)
    private Integer maxBorrowCount = 5;

    @Column(name = "max_borrow_days", nullable = false)
    private Integer maxBorrowDays = 30;

    @Column(name = "reservation_hours", nullable = false)
    private Integer reservationHours = 48;

    @Column(name = "allow_renew", nullable = false)
    private Boolean allowRenew = true;

    @Column(name = "max_renew_count", nullable = false)
    private Integer maxRenewCount = 2;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
