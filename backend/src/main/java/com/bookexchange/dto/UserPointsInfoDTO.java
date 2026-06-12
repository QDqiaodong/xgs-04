package com.bookexchange.dto;

import lombok.Data;

@Data
public class UserPointsInfoDTO {

    private Long userId;

    private String username;

    private String nickname;

    private Integer totalPoints;

    private String levelCode;

    private String levelName;

    private Integer maxBorrowCount;

    private Integer nextLevelPoints;

    private String nextLevelName;

    private Integer currentActiveBorrows;
}
