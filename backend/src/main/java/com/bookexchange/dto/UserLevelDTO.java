package com.bookexchange.dto;

import lombok.Data;

@Data
public class UserLevelDTO {

    private Long id;

    private String code;

    private String name;

    private Integer minPoints;

    private Integer maxBorrowCount;

    private Integer sortOrder;

    private String icon;

    private String description;
}
