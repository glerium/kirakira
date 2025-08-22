package com.kirakira.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);


    public MonitorService(GroupUserRepository groupUserRepository, SubmissionRepository submissionRepository, OverflowClient overflowClient, CodeforcesClient codeforcesClient) {
        this.groupUserRepository = groupUserRepository;
        this.codeforcesClient = codeforcesClient;
        this.overflowClient = overflowClient;
        this.submissionRepository = submissionRepository;
    }


    public void checkRecentSubmissionsAndNotify() {
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
                
                String realCfId = null;

                for (CfSubmissionDto submission : submissions) {
                    CfProblemDto problem = submission.getProblem();
                    String problemId = problem.getContestId() + problem.getIndex();

                    // 如果提交已经存在，则跳过
                    if (submissionRepository.checkIfUserFinishedProblem(problemId, cfId)) {
                        continue;
                    }
                    log.debug("Get submission " + submission.getId());

                    // 获取题目作者的成员信息，找出匹配的 cfId
                    List<CfMemberDto> members = submission.getAuthor().getMembers();

                    for (CfMemberDto member : members) {
                        String handle = member.getHandle();
                        if (handle.equalsIgnoreCase(cfId)) {
                            realCfId = handle;
                            break;
                        }
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
                for (String groupId : groupList) {
                    groupErrorMessages.putIfAbsent(groupId, new ArrayList<>());
                    groupErrorMessages.get(groupId).add("CodeForces API请求失败：用户 " + cfId + " 不存在！已将其从数据库中移除。");
                    groupUserRepository.removeGroupUser(groupId, cfId);
                }
            } catch (CodeforcesApiException e) {
                groupErrorMessages.putIfAbsent("123456789", new ArrayList<>());
                groupErrorMessages.get("123456789").add("CodeForces API请求失败：" + e.getLocalizedMessage());
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
                String response = overflowClient.sendSubmissionToGroup(groupId, codeforcesIds, problemInfos);
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.optInt("retcode", -1) == 0) {
                    log.info("Successfully sent submission to group " + groupId);
                } else {
                    log.warn("Error sending submission to group " + groupId + ": " + response);
                }
            }
            
            List<String> errorMessages = groupErrorMessages.get(groupId);
            if(errorMessages != null && !errorMessages.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
}
