package com.kirakira.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kirakira.client.CodeforcesClient;
import com.kirakira.client.OverflowClient;
import com.kirakira.entity.Submission;
import com.kirakira.entity.dto.codeforces.CfMemberDto;
import com.kirakira.entity.dto.codeforces.CfProblemDto;
import com.kirakira.entity.dto.codeforces.CfSubmissionDto;
import com.kirakira.entity.exception.CodeforcesApiException;
import com.kirakira.entity.exception.UserNotFoundException;
import com.kirakira.repository.GroupUserRepository;
import com.kirakira.repository.SubmissionRepository;

@Service
public class MonitorService {
    
    private final GroupUserRepository groupUserRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeforcesClient codeforcesClient;
    private final OverflowClient overflowClient;
    private final String errorNotificationGroupId;

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);


    public MonitorService(GroupUserRepository groupUserRepository, 
                         SubmissionRepository submissionRepository, 
                         OverflowClient overflowClient, 
                         CodeforcesClient codeforcesClient,
                         @Value("${bot.error.notification.group.id:}") String errorNotificationGroupId) {
        this.groupUserRepository = groupUserRepository;
        this.codeforcesClient = codeforcesClient;
        this.overflowClient = overflowClient;
        this.submissionRepository = submissionRepository;
        this.errorNotificationGroupId = errorNotificationGroupId;
    }


    public void checkRecentSubmissionsAndNotify() {
        // 检查机器人连接状态
        if (!overflowClient.isConnected()) {
            log.warn("机器人未连接，跳过本次检查");
            return;
        }
        
        // 存储每个群组对应的 Codeforces IDs 和 ProblemInfos
        Map<String, List<String>> groupCodeforcesIds = new HashMap<>();
        Map<String, List<String>> groupProblemInfos = new HashMap<>();
        Map<String, List<String>> groupErrorMessages = new HashMap<>();

        // 获取所有的 codeforces_id
        List<String> allCfIds = groupUserRepository.enumerateAllCodeforcesId();

        // 遍历每个 Codeforces ID
        log.info("Checking submissions");
        for (String cfId : allCfIds) {
            // 获取此 Codeforces ID 所在的所有群组
            List<String> groupList = groupUserRepository.enumerateGroupsByCodeforcesId(cfId);

            try {
                List<CfSubmissionDto> submissions = codeforcesClient.getRecentSubmissions(cfId);

                for (CfSubmissionDto submission : submissions) {
                    // 检查 problem 是否为 null
                    CfProblemDto problem = submission.getProblem();
                    if (problem == null) {
                        log.warn("Submission {} has null problem, skipping", submission.getId());
                        continue;
                    }
                    
                    String problemId = problem.getContestId() + problem.getIndex();

                    // 如果提交已经存在，则跳过
                    if (submissionRepository.checkIfUserFinishedProblem(problemId, cfId)) {
                        continue;
                    }
                    log.debug("Get submission " + submission.getId());

                    // 检查 author 和 members 是否为 null
                    if (submission.getAuthor() == null) {
                        log.warn("Submission {} has null author, skipping", submission.getId());
                        continue;
                    }
                    
                    List<CfMemberDto> members = submission.getAuthor().getMembers();
                    if (members == null || members.isEmpty()) {
                        log.warn("Submission {} has null or empty members, skipping", submission.getId());
                        continue;
                    }

                    // 获取题目作者的成员信息，找出匹配的 cfId
                    String realCfId = null;
                    for (CfMemberDto member : members) {
                        String handle = member.getHandle();
                        if (handle != null && handle.equalsIgnoreCase(cfId)) {
                            realCfId = handle;
                            break;
                        }
                    }

                    // 如果没有找到匹配的 cfId，跳过此提交
                    if (realCfId == null) {
                        log.warn("No matching cfId found for submission {}", submission.getId());
                        continue;
                    }

                    // 获取题目信息
                    String problemInfo = problemId + " (" + problem.getRatingStr() + ")";

                    for (String groupId : groupList) {
                        // 维护群组的 Codeforces ID 列表
                        groupCodeforcesIds.putIfAbsent(groupId, new ArrayList<>());
                        groupProblemInfos.putIfAbsent(groupId, new ArrayList<>());

                        groupCodeforcesIds.get(groupId).add(realCfId);
                        groupProblemInfos.get(groupId).add(problemInfo);
                    }

                    // 将提交信息插入数据库
                    Submission submissionDb = Submission.builder()
                            .codeforcesId(cfId)
                            .problemId(problem.getContestId() + problem.getIndex())
                            .submissionId(submission.getId())
                            .submissionTime(submission.getCreationTime())
                            .build();
                    submissionRepository.insertSubmission(submissionDb);
                }
            } catch (UserNotFoundException e) {
                log.warn("用户 {} 不存在，从数据库中移除", cfId);
                for (String groupId : groupList) {
                    groupErrorMessages.putIfAbsent(groupId, new ArrayList<>());
                    groupErrorMessages.get(groupId).add("CodeForces API请求失败：用户 " + cfId + " 不存在！已将其从数据库中移除。");
                    groupUserRepository.removeGroupUser(groupId, cfId);
                }
            } catch (CodeforcesApiException e) {
                // 记录 API 错误到日志，并发送到配置的错误通知群组
                log.error("CodeForces API请求失败 (用户: {}): {}", cfId, e.getLocalizedMessage(), e);
                if (errorNotificationGroupId != null && !errorNotificationGroupId.isEmpty()) {
                    groupErrorMessages.putIfAbsent(errorNotificationGroupId, new ArrayList<>());
                    groupErrorMessages.get(errorNotificationGroupId).add("CodeForces API请求失败 (用户: " + cfId + "): " + e.getLocalizedMessage());
                }
            } catch (RuntimeException e) {
                // 捕获运行时异常，防止单个用户的错误影响整体流程
                log.error("处理用户 {} 的提交时发生运行时错误: {}", cfId, e.getMessage(), e);
                if (errorNotificationGroupId != null && !errorNotificationGroupId.isEmpty()) {
                    groupErrorMessages.putIfAbsent(errorNotificationGroupId, new ArrayList<>());
                    groupErrorMessages.get(errorNotificationGroupId).add("处理用户 " + cfId + " 时发生错误: " + e.getMessage());
                }
            }
        }
        
        log.info("Check submissions done.");
        // 对每个群组发送消息，并控制消息发送间隔
        for (String groupId : groupCodeforcesIds.keySet()) {
            List<String> codeforcesIds = groupCodeforcesIds.get(groupId);
            List<String> problemInfos = groupProblemInfos.get(groupId);
            if (!codeforcesIds.isEmpty() && !problemInfos.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 再次检查连接状态
                if (!overflowClient.isConnected()) {
                    log.error("发送消息前检测到机器人未连接，跳过发送");
                    break;
                }
                
                String response = overflowClient.sendSubmissionToGroup(groupId, codeforcesIds, problemInfos);
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.optInt("retcode", -1) == 0) {
                    log.info("Successfully sent submission to group " + groupId);
                } else {
                    log.warn("Error sending submission to group " + groupId + ": " + response);
                }
            }
            
            sendErrorMessagesToGroup(groupId, groupErrorMessages.get(groupId));
        }
        
        // 处理未在 groupCodeforcesIds 中但有错误消息的群组（如错误通知群组）
        for (String groupId : groupErrorMessages.keySet()) {
            if (!groupCodeforcesIds.containsKey(groupId)) {
                sendErrorMessagesToGroup(groupId, groupErrorMessages.get(groupId));
            }
        }
    }

    /**
     * 向指定群组发送错误消息
     * @param groupId 群组 ID
     * @param errorMessages 错误消息列表
     */
    private void sendErrorMessagesToGroup(String groupId, List<String> errorMessages) {
        if(errorMessages != null && !errorMessages.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 检查连接状态
            if (!overflowClient.isConnected()) {
                log.error("发送错误消息前检测到机器人未连接，跳过发送");
                return;
            }
            
            String response = overflowClient.sendErrorMessageToGroup(groupId, errorMessages);
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.optInt("retcode", -1) == 0) {
                log.info("Successfully sent error message to group " + groupId);
            } else {
                log.warn("Error sending error message to group " + groupId + ": " + response);
            }
        }
    }
}
