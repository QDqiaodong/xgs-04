package com.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBorrowRankDTO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private Long borrowCount;

    private Long lendCount;

    private Long totalActivity;

    private Integer rank;
}
