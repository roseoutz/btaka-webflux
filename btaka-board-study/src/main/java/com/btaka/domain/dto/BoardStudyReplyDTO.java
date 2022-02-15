package com.btaka.domain.dto;

import com.btaka.domain.entity.BoardStudyReplyEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardStudyReplyDTO {

    public BoardStudyReplyDTO(BoardStudyReplyEntity entity) {
        this.oid = entity.getOid();
        this.postOid = entity.getPostOid();
        this.parentOid = entity.getParentOid();
        this.reply = entity.getReply();
        this.likes = entity.getLikes();
        this.insertUser = entity.getInsertUser();
        this.insertTime = entity.getInsertTime() == null ? LocalDateTime.now() : entity.getInsertTime();
        this.updateTime = entity.getUpdateTime() == null ? this.insertTime : entity.getUpdateTime();
    }

    private String oid;
    private String postOid;
    private String parentOid;
    private String reply ;
    private int likes;
    private String insertUser;
    private LocalDateTime insertTime;
    private LocalDateTime updateTime;
}
