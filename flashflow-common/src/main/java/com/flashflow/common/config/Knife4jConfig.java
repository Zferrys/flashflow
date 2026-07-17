package com.flashflow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger 配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI flashflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlashFlow API")
                        .description("高并发闪购订单平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Zferrys")
                                .url("https://github.com/Zferrys")));
    }
}
