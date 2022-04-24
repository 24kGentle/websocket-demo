package com.wy.websocketdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @Author wangyang
 * @Date 2020/9/3 17:01
 */
@Configuration
@EnableWebSocket  // 开启websocket
public class WebSocketConfiguration {

    @Bean   // 实例化ServerEndPoint的bean，该Bean会自动注册@ServerEndPoint注解声明的端点
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
