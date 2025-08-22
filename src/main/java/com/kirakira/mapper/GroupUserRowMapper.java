package com.kirakira.mapper;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.kirakira.entity.GroupUser;

public class GroupUserRowMapper implements RowMapper<GroupUser> {
    @Override
    public GroupUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        GroupUser groupUser = new GroupUser();
        groupUser.setId(rs.getInt("id"));
        groupUser.setGroupId(rs.getString("group_id"));
        groupUser.setUserQqId(rs.getString("user_qq_id"));
        groupUser.setCodeforcesId(rs.getString("codeforces_id"));
        return groupUser;
    }
}