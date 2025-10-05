package com.transportation.dispatch.config;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Slf4j
public class DocumentationConfig {

    @Bean
    CommandLineRunner commandLineRunner(Environment env) {
        return args -> {
            String protocol = "http";
            if (env.getProperty("server.ssl.key-store") != null) {
                protocol = "https";
            }
            String serverPort = env.getProperty("server.port");
            String contextPath = env.getProperty("server.servlet.context-path");
            if (StringUtils.isBlank(contextPath)) {
                contextPath = "/doc.html";
            } else {
                contextPath = contextPath + "/doc.html";
            }
            String hostAddress = "localhost";
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.warn("The host name could not be determined, using `localhost` as fallback");
            }
            log.info("""
                        ----------------------------------------------------------
                        \t应用程序“{}”正在运行中......
                        \t接口文档访问 URL:
                        \t本地: \t\t{}://localhost:{}{}
                        \t外部: \t{}://{}:{}{}
                        \t配置文件: \t{}
                        ----------------------------------------------------------""",
                    env.getProperty("spring.application.name"),
                    protocol,
                    serverPort,
                    contextPath,
                    protocol,
                    hostAddress,
                    serverPort,
                    contextPath,
                    env.getActiveProfiles());
        };
    }
}