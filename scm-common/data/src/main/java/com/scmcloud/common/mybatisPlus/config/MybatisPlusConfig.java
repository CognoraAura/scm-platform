package com.scmcloud.common.mybatisPlus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.scmcloud.common.mybatisPlus.handler.UUIDTypeHandler;
import com.scmcloud.common.mybatisPlus.properties.MybatisPlusProperties;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * MybatisPlus 配置�?
 *
 * @author Deng
 * createData 2025/10/15 13:35
 * @version 1.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(properties.getPaginationMaxLimit()); // 最大单页限制数�?
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否进行处理
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插�?
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防止全表更新与删除插�?
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
    
    /**
     * 注册自定义类型处理器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 注册 UUID类型处理�?
            typeHandlerRegistry.register(UUID.class, UUIDTypeHandler.class);
        };
    }

    @Bean
    public MybatisPlusProperties mybatisPlusProperties() {
        return new MybatisPlusProperties();
    }
}
