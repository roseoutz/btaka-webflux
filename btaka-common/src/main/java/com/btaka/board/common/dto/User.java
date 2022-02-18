package com.btaka.board.common.dto;

import com.btaka.board.common.constants.Roles;
import lombok.*;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String oid;
    private String email;
    private String username;
    private String mobile;
    private String oauthId;
    private Roles roles;

}