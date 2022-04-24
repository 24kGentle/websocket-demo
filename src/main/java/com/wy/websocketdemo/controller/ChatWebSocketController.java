package com.wy.websocketdemo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wy.websocketdemo.pojo.MyMessage;
import com.wy.websocketdemo.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author wangyang
 * @Date 2020/9/3 17:04
 */
@ServerEndpoint("/chat/{userId}")
@Component
public class ChatWebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 当前连接数
     */
    private static AtomicInteger onlineCount = new AtomicInteger(0);

    /**
     *  用来存放每个客户端对应的WebSocketServer对象
     */
    private static ConcurrentHashMap<String, ChatWebSocketController> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 用来存放每个客户端对应的ChatWebSocketController对象。
     */
    private static CopyOnWriteArrayList<ChatWebSocketController> copyOnWriteArrayList = new CopyOnWriteArrayList();

    /**
     * 与某一个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接受 userId
     */
    private String userId = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            onlineCount.incrementAndGet();
            copyOnWriteArrayList.add(this);
        }
        LOGGER.info("用户连接：" + userId + ",当前在线人数为：" + getOnlineCount());
        try {
            sendMessage("连接成功！");
        } catch (Exception e) {
            LOGGER.error("用户：" + userId + ", 网络异常！！！！");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            onlineCount.getAndDecrement();
            copyOnWriteArrayList.remove(this);
        }
        LOGGER.info("用户退出：" + userId + ", 当前在线人数为：" + getOnlineCount());
    }

    /**
     * 受到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message) {
        LOGGER.info("用户消息：" + userId + ",报文：" + message);
        if (!StringUtils.isEmpty(message)) {
            try {
                MyMessage myMessage = JSON.parseObject(message, MyMessage.class);
                String messageContent = myMessage.getMessage();
                String messageType = myMessage.getMessageType();

                // 单独聊天
                if ("1".equals(messageType)) {
                    // 消息接收者
                    String recUser = myMessage.getUserId();
                    // 输入框实际内容
                    sendInfo(messageContent, recUser, userId);
                }

                // 群聊
                if ("2".equals(messageType)) {
                    sendGroupInfo(messageContent, userId);
                }

            } catch (Exception e) {
                LOGGER.error("解析失败={}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Throwable error) {
        LOGGER.error("用户错误：" + this.userId + "，原因：" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static synchronized AtomicInteger getOnlineCount() {
        return onlineCount;
    }



    /**
     * 单聊
     * @param messageContent
     * @param recUserId
     * @param userId
     */
    public void sendInfo(String messageContent, String recUserId, String userId) throws IOException {
        JSONObject result = new JSONObject();

        for (ChatWebSocketController chatWebSocketController : copyOnWriteArrayList) {
            if(recUserId.equals(chatWebSocketController.userId)) {
                LOGGER.info("给用户：" + recUserId + "传递消息：" + messageContent);
                result.put("message", messageContent);
                result.put("sendUserId", userId);
                chatWebSocketController.sendMessage(result.toJSONString());
            }
        }
    }


    /**
     * 群聊
     * @param messageContent
     * @param userId
     */
    public void sendGroupInfo(String messageContent, String userId) throws IOException {
        JSONObject result = new JSONObject();

        Iterator<ChatWebSocketController> iterator = copyOnWriteArrayList.iterator();
        while (iterator.hasNext()) {
            ChatWebSocketController chatWebSocketController = iterator.next();
            if (!userId.equals(chatWebSocketController.userId)) {
                result.put("message", messageContent);
                result.put("sendUserId", userId);
                chatWebSocketController.sendMessage(result.toJSONString());
            }

        }
    }

}
