package com.bookexchange.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @Column(length = 200)
    private String avatar;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false, length = 20)
    private String level = "BRONZE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
