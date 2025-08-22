package com.kirakira.listener;

import com.kirakira.service.BotService;

import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.BotEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;


public class GroupMessageListener {
    private final Listener<GroupMessageEvent> listener;
    
    public GroupMessageListener(BotService botService, EventChannel<BotEvent> channel) {
        this.listener = channel.subscribeAlways(GroupMessageEvent.class, event -> {
            String message = event.getMessage().contentToString();
            MemberPermission permission = event.getPermission();
            String groupId = Long.toString(event.getGroup().getId());
            String senderId = Long.toString(event.getSender().getId());

            if (!message.startsWith("/")) {
                return;
            }

            message = message.substring(1);
            String[] argv = message.split(" ");
            
            String returnMsg = "null";

            if (argv[0].equals("bind")) {
                if (argv.length != 3 || !argv[1].equals("cf")) {
                    returnMsg = "指令格式错误：/bind cf [codeforces_id]";
                } else {
                    returnMsg = botService.linkAccount(groupId, senderId, argv[2]);
                }
            } else if (argv[0].equals("unbind")) {
                if (argv.length != 3 || !argv[1].equals("cf")) {
                    returnMsg = "指令格式错误：/unbind cf [codeforces_id]";
                } else {
                    returnMsg = botService.unlinkAccount(groupId, senderId, argv[2]);
                }
            } else if (argv[0].equals("help")) {
                returnMsg = botService.getHelp();
            } else if (argv[0].equals("list")) {
                if (argv.length != 2 || !argv[1].equals("cf")) {
                    returnMsg = "指令格式错误：/list cf";
                } else {
                    returnMsg = botService.querySingleUserList(groupId, senderId);
                }
            } else if (argv[0].equals("listall")) {
                if (argv.length != 2 || !argv[1].equals("cf")) {
                    returnMsg = "指令格式错误：/listall cf";
                } else {
                    if (permission != MemberPermission.ADMINISTRATOR && permission != MemberPermission.OWNER) {
                        returnMsg = "权限不够，本操作至少需要管理员权限！";
                    } else {
                        returnMsg = botService.queryAllUserList(groupId);
                    }
                }
            } else {        // 未知指令，忽略消息
                return;
            }

            var subject = event.getSubject();

            subject.sendMessage(returnMsg);
        });
    }

    public Listener<GroupMessageEvent> getListener() {
        return listener;
    }
}
