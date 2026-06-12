package com.bookexchange.dto;

import lombok.Data;

@Data
public class PointsRecordQueryDTO {

    private Long userId;

    private String type;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
