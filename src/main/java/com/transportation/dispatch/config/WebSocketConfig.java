package com.transportation.dispatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 开启 WebSocket 消息代理功能
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 配置消息代理（Broker）
        //    这里我们启用一个简单（基于内存）的消息代理。
        //    所有目的地以 "/topic" 开头的消息都会被路由到这个代理上。
        config.enableSimpleBroker("/topic");

        // 2. 配置应用的目的地前缀
        //    客户端发送消息的目的地前缀，所有发往 @MessageMapping 注解方法的消息都需要带上这个前缀。
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. 注册 STOMP 端点（Endpoint）
        //    这是客户端用于连接 WebSocket 服务器的地址。
        //    "/ws" 是我们暴露的端点。
        //    withSockJS() 提供了对不支持 WebSocket 的浏览器的降级方案。
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }
}
