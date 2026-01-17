package com.kirakira.client;

import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kirakira.listener.GroupMessageListener;
import com.kirakira.service.BotService;

import top.mrxiaom.overflow.BotBuilder;
import top.mrxiaom.overflow.contact.RemoteBot;
import net.mamoe.mirai.Bot;

@Component
public class OverflowClient {
    RemoteBot bot;
    Bot miraibot;
    GroupMessageListener groupMsgListener;
    BotService botService;

    public OverflowClient(BotService botService, 
                          @Value("${bot.websocket.url}") String websocketUrl,
                          @Value("${bot.websocket.token}") String token) {
        this.miraibot = BotBuilder.positive(websocketUrl)
            .token(token)
            .connect();
        this.bot = (RemoteBot) this.miraibot;

        this.groupMsgListener = new GroupMessageListener(botService, miraibot.getEventChannel());
    }

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
        var response = bot.executeAction("send_group_msg", jsonText);

        return response;
    }

    public String sendErrorMessageToGroup(String groupId, List<String> errorMessages) {
        boolean hasError = false;
        JSONObject finalResponse = new JSONObject();
        
        for (String error : errorMessages) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("group_id", groupId);
            jsonObject.put("message", error);
            String rawResponse = bot.executeAction("send_group_msg", jsonObject.toString());
            // System.out.println(rawResponse.toString());
            JSONObject response = new JSONObject(rawResponse);
            
            int retcode = response.optInt("retcode", -1);
            if (retcode != 0) {
                hasError = true;
                finalResponse = response;
            } else if (!hasError) {
                finalResponse = response;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                finalResponse.put("retcode", -2)
                             .put("message", "操作被中断");
                break;
            }
        }
        
        return finalResponse.toString();
    
    }
}