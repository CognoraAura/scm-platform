package com.scmcloud.common.mybatisPlus.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "frog.mybatis")
@Data
public class MybatisPlusProperties {
    /**
     * 鏈€澶у崟椤甸檺鍒舵暟閲忥紝榛樿 1000锟?
     */
    private Long paginationMaxLimit = 1000L;
}

