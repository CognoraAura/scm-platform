package com.scmcloud.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scmcloud.common.web.serializer.SensitiveJsonSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 娉ㄥ唽鑴辨晱搴忓垪鍖栧櫒
        SimpleModule module = new SimpleModule();
        module.addSerializer(String.class, new SensitiveJsonSerializer());
        objectMapper.registerModule(module);
        
        // 娉ㄥ唽JavaTimeModule浠ユ敮鎸丣ava 8鏃堕棿绫诲瀷搴忓垪锟?
        objectMapper.registerModule(new JavaTimeModule());
        
        return objectMapper;
    }
}