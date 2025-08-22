package com.kirakira.entity;

import lombok.Data;

@Data
public class GroupUser {
    private Integer id;
    private String groupId;
    private String userQqId;
    private String codeforcesId;

    public void setCodeforcesId(String codeforcesId) {
        if (codeforcesId != null) {
            this.codeforcesId = codeforcesId.toLowerCase();
        } else {
            this.codeforcesId = null;
        }
    }
}
