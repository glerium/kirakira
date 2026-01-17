package com.kirakira.client;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import net.mamoe.mirai.event.events.BotOfflineEvent;
import net.mamoe.mirai.event.events.BotOnlineEvent;

@Component
public class OverflowClient {
    private static final Logger log = LoggerFactory.getLogger(OverflowClient.class);
    
    RemoteBot bot;
    Bot miraibot;
    GroupMessageListener groupMsgListener;
    BotService botService;
    
    private final String websocketUrl;
    private final String token;
    private volatile boolean isConnected = false;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean shouldReconnect = true;

    public OverflowClient(BotService botService, 
                          @Value("${bot.websocket.url}") String websocketUrl,
                          @Value("${bot.websocket.token}") String token) {
        this.botService = botService;
        this.websocketUrl = websocketUrl;
        this.token = token;
        
        // 初始化连接
        connect();
        
        // 启动心跳检测
        startHeartbeatMonitor();
    }
    
    /**
     * 建立或重新建立WebSocket连接
     */
    private synchronized void connect() {
        try {
            log.info("正在连接到WebSocket服务器: {}", websocketUrl);
            
            this.miraibot = BotBuilder.positive(websocketUrl)
                .token(token)
                .connect();
            this.bot = (RemoteBot) this.miraibot;
            
            // 注册事件监听器
            setupEventListeners();
            
            this.groupMsgListener = new GroupMessageListener(botService, miraibot.getEventChannel());
            
            isConnected = true;
            log.info("成功连接到WebSocket服务器");
            
        } catch (Exception e) {
            log.error("连接到WebSocket服务器失败: {}", e.getMessage(), e);
            isConnected = false;
            scheduleReconnect();
        }
    }
    
    /**
     * 设置事件监听器，监听上线和离线事件
     */
    private void setupEventListeners() {
        if (miraibot != null) {
            // 监听机器人上线事件
            miraibot.getEventChannel().subscribeAlways(BotOnlineEvent.class, event -> {
                log.info("机器人已上线，账号ID: {}", event.getBot().getId());
                isConnected = true;
            });
            
            // 监听机器人离线事件
            miraibot.getEventChannel().subscribeAlways(BotOfflineEvent.class, event -> {
                log.warn("机器人已离线，原因: {}", event.getClass().getSimpleName());
                isConnected = false;
                
                // 触发重连
                if (shouldReconnect) {
                    scheduleReconnect();
                }
            });
        }
    }
    
    /**
     * 调度重连任务
     */
    private void scheduleReconnect() {
        reconnectScheduler.schedule(() -> {
            if (!isConnected && shouldReconnect) {
                log.info("尝试重新连接到WebSocket服务器...");
                connect();
            }
        }, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 启动心跳监测，定期检查连接状态
     */
    private void startHeartbeatMonitor() {
        reconnectScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!isConnected || miraibot == null || !miraibot.isOnline()) {
                    log.warn("心跳检测发现连接已断开，尝试重连");
                    isConnected = false;
                    connect();
                }
            } catch (Exception e) {
                log.error("心跳检测时出错: {}", e.getMessage(), e);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 检查连接是否可用
     */
    public boolean isConnected() {
        return isConnected && miraibot != null && miraibot.isOnline();
    }

    public String sendSubmissionToGroup(String groupId, List<String> codeforcesIds, List<String> problemInfos) {
        if (codeforcesIds.size() != problemInfos.size()) {
            throw new IllegalArgumentException("Codeforces ID 列表和 Problem Info 列表的长度必须相同");
        }
        
        if (!isConnected()) {
            log.error("无法发送消息：机器人未连接");
            return "{\"retcode\": -1, \"message\": \"机器人未连接\"}";
        }

        try {
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
        } catch (Exception e) {
            log.error("发送消息到群组 {} 时出错: {}", groupId, e.getMessage(), e);
            return "{\"retcode\": -1, \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    public String sendErrorMessageToGroup(String groupId, List<String> errorMessages) {
        if (!isConnected()) {
            log.error("无法发送错误消息：机器人未连接");
            return "{\"retcode\": -1, \"message\": \"机器人未连接\"}";
        }
        
        boolean hasError = false;
        JSONObject finalResponse = new JSONObject();
        
        for (String error : errorMessages) {
            try {
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
            } catch (Exception e) {
                log.error("发送错误消息到群组 {} 时出错: {}", groupId, e.getMessage(), e);
                finalResponse.put("retcode", -1)
                             .put("message", e.getMessage());
                hasError = true;
            }
        }
        
        return finalResponse.toString();
    
    }
    
    /**
     * 关闭客户端，停止重连
     */
    public void shutdown() {
        shouldReconnect = false;
        reconnectScheduler.shutdown();
        try {
            if (miraibot != null && miraibot.isOnline()) {
                miraibot.close();
            }
        } catch (Exception e) {
            log.error("关闭机器人时出错: {}", e.getMessage(), e);
        }
    }
}