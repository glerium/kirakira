package com.kirakira.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kirakira.client.CodeforcesClient;
import com.kirakira.entity.GroupUser;
import com.kirakira.repository.GroupUserRepository;

@Component
public class BotService {

    private final GroupUserRepository groupUserRepository;
    private final CodeforcesClient codeforcesClient;

    @Autowired
    public BotService(GroupUserRepository groupUserRepository, CodeforcesClient codeforcesClient) {
        this.groupUserRepository = groupUserRepository;
        this.codeforcesClient = codeforcesClient;
    }

    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("/bind cf [codeforces_id]: 绑定CF账号\n");
        sb.append("/unbind cf [codeforces_id]: 解绑CF账号\n");
        sb.append("/list cf: 列出自己绑定的CF账号\n");
        sb.append("/listall cf: 列出所有人绑定的CF账号\n");
        return sb.toString();
    }

    public String linkAccount(String groupId, String qqId, String codeforcesId) {
        // 1. 检查 Codeforces ID 是否已存在
        if (groupUserRepository.checkIfCodeforcesIdExists(groupId, codeforcesId)) {
            return "Codeforces ID 已被绑定";
        }
        if (codeforcesId == null || !codeforcesId.matches("^[a-zA-Z0-9_-]+$")) {
            return "账号绑定失败：handle should contain only Latin letters, digits, underscore or dash characters";
        }

        // check if user exists
        try {   
            var result = codeforcesClient.getRecentSubmissions(codeforcesId);
        } catch (Exception e) {
            return "账号绑定失败：" + e.getLocalizedMessage();
        }

        // 2. 创建用户对象
        GroupUser user = new GroupUser();
        user.setGroupId(groupId);
        user.setUserQqId(qqId);
        user.setCodeforcesId(codeforcesId);

        // 3. 写入数据库
        boolean success = groupUserRepository.addGroupUser(user);
        return success ? "账号绑定成功" : "账号绑定失败";
    }

    public String queryAllUserList(String groupId) {
        Map<String, List<String>> userList = groupUserRepository.enumerateCodeforcesIdFromGroup(groupId);

        if (userList.isEmpty()) {
            return "该群组内暂无绑定的 Codeforces 账号。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("群内 Codeforces 绑定列表：\n");

        for (Map.Entry<String, List<String>> entry : userList.entrySet()) {
            sb.append(entry.getKey()).append(" -> ");
            sb.append(String.join(", ", entry.getValue())); // 连接 Codeforces ID
            sb.append("\n");
        }

        return sb.toString();
    }

    public String querySingleUserList(String groupId, String qqId) {
        List<String> codeforcesIds = groupUserRepository.enumerateCodeforcesIdOfSingleUser(groupId, qqId);

        if (codeforcesIds.isEmpty()) {
            return "你还没有绑定 CodeForces 账号！";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(qqId + " 绑定的 CodeForces 账号如下：\n");
        sb.append(String.join(", ", codeforcesIds));
        
        return sb.toString();
    }

    /**
     * 解绑 Codeforces 账号
     * @param request 包含 groupId, qqId, codeforcesId 的请求体
     * @return 解绑操作的结果
     */
    public String unlinkAccount(String groupId, String qqId, String codeforcesId) {
        // 1. 检查是否存在该绑定
        if (!groupUserRepository.checkIfBindingExists(groupId, qqId, codeforcesId)) {
            return "该 Codeforces ID 未绑定到此 QQ 号";
        }

        // 2. 执行删除操作
        boolean success = groupUserRepository.removeGroupUser(groupId, codeforcesId);
        return success ? "解绑成功" : "数据库操作失败";
    }
}
