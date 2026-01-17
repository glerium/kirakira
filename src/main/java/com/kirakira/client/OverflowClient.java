package com.kirakira.client;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kirakira.listener.GroupMessageListener;
import com.kirakira.service.BotService;

import top.mrxiaom.overflow.BotBuilder;
import top.mrxiaom.overflow.contact.RemoteBot;
import net.mamoe.mirai.Bot;

@Component
public class OverflowClient {
    private static final Logger log = LoggerFactory.getLogger(OverflowClient.class);
    
    RemoteBot bot;
    Bot miraibot;
    GroupMessageListener groupMsgListener;
    BotService botService;
    private final long messageSendIntervalMs;

    public OverflowClient(BotService botService, 
                          @Value("${bot.websocket.url}") String websocketUrl,
                          @Value("${bot.websocket.token}") String token,
                          @Value("${message.send.interval.ms:1000}") long messageSendIntervalMs) {
        this.miraibot = BotBuilder.positive(websocketUrl)
            .token(token)
            .connect();
        this.bot = (RemoteBot) this.miraibot;
        this.messageSendIntervalMs = messageSendIntervalMs;

        this.groupMsgListener = new GroupMessageListener(botService, miraibot.getEventChannel());
    }

    /**
     * 向指定群组发送提交通知消息
     * @param groupId 群组 ID
     * @param codeforcesIds Codeforces ID 列表
     * @param problemInfos 题目信息列表
     * @return 发送响应
     * @throws IllegalArgumentException 当两个列表长度不一致时
     */
    public String sendSubmissionToGroup(String groupId, List<String> codeforcesIds, List<String> problemInfos) {
        if (codeforcesIds.size() != problemInfos.size()) {
            throw new IllegalArgumentException("Codeforces ID 列表和 Problem Info 列表的长度必须相同");
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("group_id", groupId);

        // 构造多行消息
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < codeforcesIds.size(); i++) {
            messageBuilder.append(codeforcesIds.get(i)).append(" 通过了 ").append(problemInfos.get(i)).append("。\n");
        }

        jsonObject.put("message", messageBuilder.toString().trim()); // 去除末尾多余换行符

        String jsonText = jsonObject.toString();
        String response = bot.executeAction("send_group_msg", jsonText);

        return response;
    }

    /**
     * 向指定群组发送错误消息列表
     * @param groupId 群组 ID
     * @param errorMessages 错误消息列表
     * @return 最终的发送响应
     */
    public String sendErrorMessageToGroup(String groupId, List<String> errorMessages) {
        boolean hasError = false;
        JSONObject finalResponse = new JSONObject();
        
        for (String error : errorMessages) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("group_id", groupId);
            jsonObject.put("message", error);
            String rawResponse = bot.executeAction("send_group_msg", jsonObject.toString());
            JSONObject response = new JSONObject(rawResponse);
            
            int retcode = response.optInt("retcode", -1);
            if (retcode != 0) {
                hasError = true;
                finalResponse = response;
            } else if (!hasError) {
                finalResponse = response;
            }
            
            try {
                Thread.sleep(messageSendIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Message sending interrupted for group {}", groupId, e);
                finalResponse.put("retcode", -2)
                             .put("message", "操作被中断");
                break;
            }
        }
        
        return finalResponse.toString();
    
    }
}