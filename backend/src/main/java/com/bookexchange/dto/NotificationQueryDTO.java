package com.bookexchange.dto;

import lombok.Data;

@Data
public class NotificationQueryDTO {

    private Long userId;
    private String type;
    private Boolean isRead;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
