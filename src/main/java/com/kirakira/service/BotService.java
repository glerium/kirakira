package com.kirakira.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kirakira.client.CodeforcesClient;
import com.kirakira.entity.GroupUser;
import com.kirakira.entity.exception.CodeforcesApiException;
import com.kirakira.entity.exception.UserNotFoundException;
import com.kirakira.repository.GroupUserRepository;

@Component
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    private static final Logger operationLog = LoggerFactory.getLogger("com.kirakira.operation");

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
        operationLog.info("BIND - Group: {}, QQ: {}, CF: {} - START", groupId, qqId, codeforcesId);
        
        // 1. 检查 Codeforces ID 是否已存在
        if (groupUserRepository.checkIfCodeforcesIdExists(groupId, codeforcesId)) {
            operationLog.info("BIND - Group: {}, QQ: {}, CF: {} - FAILED: Already bound", groupId, qqId, codeforcesId);
            return "Codeforces ID 已被绑定";
        }
        if (codeforcesId == null || !codeforcesId.matches("^[a-zA-Z0-9_-]+$")) {
            operationLog.info("BIND - Group: {}, QQ: {}, CF: {} - FAILED: Invalid handle format", groupId, qqId, codeforcesId);
            return "账号绑定失败：handle should contain only Latin letters, digits, underscore or dash characters";
        }

        // check if user exists
        try {
            // Call API to verify user exists (result intentionally discarded)
            codeforcesClient.getRecentSubmissions(codeforcesId);
        } catch (UserNotFoundException e) {
            operationLog.info("BIND - Group: {}, QQ: {}, CF: {} - FAILED: User not found", groupId, qqId, codeforcesId);
            return "账号绑定失败：用户不存在";
        } catch (CodeforcesApiException e) {
            operationLog.error("BIND - Group: {}, QQ: {}, CF: {} - FAILED: API error - {}", groupId, qqId, codeforcesId, e.getMessage());
            return "账号绑定失败：" + (e.getMessage() != null ? e.getMessage() : "API 请求失败");
        } catch (Exception e) {
            operationLog.error("BIND - Group: {}, QQ: {}, CF: {} - FAILED: Unknown error - {}", groupId, qqId, codeforcesId, e.getLocalizedMessage());
            return "账号绑定失败：" + (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "未知错误");
        }

        // 2. 创建用户对象
        GroupUser user = new GroupUser();
        user.setGroupId(groupId);
        user.setUserQqId(qqId);
        user.setCodeforcesId(codeforcesId);

        // 3. 写入数据库
        boolean success = groupUserRepository.addGroupUser(user);
        if (success) {
            operationLog.info("BIND - Group: {}, QQ: {}, CF: {} - SUCCESS", groupId, qqId, codeforcesId);
        } else {
            operationLog.error("BIND - Group: {}, QQ: {}, CF: {} - FAILED: Database error", groupId, qqId, codeforcesId);
        }
        return success ? "账号绑定成功" : "账号绑定失败";
    }

    public String queryAllUserList(String groupId) {
        operationLog.info("LISTALL - Group: {}, Requesting user list", groupId);
        
        Map<String, List<String>> userList = groupUserRepository.enumerateCodeforcesIdFromGroup(groupId);

        if (userList.isEmpty()) {
            operationLog.info("LISTALL - Group: {}, Result: No bindings found", groupId);
            return "该群组内暂无绑定的 Codeforces 账号。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("群内 Codeforces 绑定列表：\n");

        for (Map.Entry<String, List<String>> entry : userList.entrySet()) {
            sb.append(entry.getKey()).append(" -> ");
            sb.append(String.join(", ", entry.getValue())); // 连接 Codeforces ID
            sb.append("\n");
        }

        operationLog.info("LISTALL - Group: {}, Result: {} users with bindings", groupId, userList.size());
        return sb.toString();
    }

    public String querySingleUserList(String groupId, String qqId) {
        operationLog.info("LIST - Group: {}, QQ: {}, Requesting bindings", groupId, qqId);
        
        List<String> codeforcesIds = groupUserRepository.enumerateCodeforcesIdOfSingleUser(groupId, qqId);

        if (codeforcesIds.isEmpty()) {
            operationLog.info("LIST - Group: {}, QQ: {}, Result: No bindings", groupId, qqId);
            return "你还没有绑定 CodeForces 账号！";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(qqId + " 绑定的 CodeForces 账号如下：\n");
        sb.append(String.join(", ", codeforcesIds));
        
        operationLog.info("LIST - Group: {}, QQ: {}, Result: {} binding(s) - {}", groupId, qqId, codeforcesIds.size(), String.join(", ", codeforcesIds));
        return sb.toString();
    }

    /**
     * 解绑 Codeforces 账号
     * @param groupId 群号
     * @param qqId QQ 号
     * @param codeforcesId Codeforces ID
     * @return 解绑操作的结果
     */
    public String unlinkAccount(String groupId, String qqId, String codeforcesId) {
        operationLog.info("UNBIND - Group: {}, QQ: {}, CF: {} - START", groupId, qqId, codeforcesId);
        
        // 1. 检查是否存在该绑定
        if (!groupUserRepository.checkIfBindingExists(groupId, qqId, codeforcesId)) {
            operationLog.info("UNBIND - Group: {}, QQ: {}, CF: {} - FAILED: Binding does not exist", groupId, qqId, codeforcesId);
            return "该 Codeforces ID 未绑定到此 QQ 号";
        }

        // 2. 执行删除操作，只删除该用户的绑定
        boolean success = groupUserRepository.removeGroupUserBinding(groupId, qqId, codeforcesId);
        if (success) {
            operationLog.info("UNBIND - Group: {}, QQ: {}, CF: {} - SUCCESS", groupId, qqId, codeforcesId);
        } else {
            operationLog.error("UNBIND - Group: {}, QQ: {}, CF: {} - FAILED: Database error", groupId, qqId, codeforcesId);
        }
        return success ? "解绑成功" : "数据库操作失败";
    }
}
