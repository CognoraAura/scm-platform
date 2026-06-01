package com.scmcloud.common.redis.config;

import com.scmcloud.common.cache.spring.TwoLevelCacheInvalidationListener;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import com.scmcloud.common.cache.spring.TwoLevelCacheManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置�?
 *
 * @author Deng
 * createData 2025/10/15 14:33
 * @version 1.0
 */
@Configuration
@EnableCaching
public class RedisConfig {
    private static final String TWOLEVEL_INVALIDATION_CHANNEL = "cache:invalidation:twolevel";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<Object> serializer = RedisSerializer.json();

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key 采用String的序列化方式
        template.setKeySerializer(stringSerializer);
        // hash 的key也采用String的序列化方式
        template.setHashKeySerializer(stringSerializer);
        // value 序列化方式采用jackson
        template.setValueSerializer(serializer);
        // hash  的value序列化方式采用jackson
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer twoLevelCacheListenerContainer(
            RedisConnectionFactory connectionFactory,
            TwoLevelCacheInvalidationListener twoLevelListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(twoLevelListener), new PatternTopic(TWOLEVEL_INVALIDATION_CHANNEL));
        return container;
    }

    @Bean
    @Primary
    public CacheManager twoLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
        Duration defaultTtl = Duration.ofHours(1);
        Map<String, Duration> ttls = new HashMap<>();
        // 用户基本信息缓存
        ttls.put("user", Duration.ofMinutes(30));
        ttls.put("userInfo", Duration.ofMinutes(30));
        ttls.put("userDetails", Duration.ofMinutes(30));

        // 权限和角色缓�?
        ttls.put("userRoles", Duration.ofHours(1));
        ttls.put("userPermissions", Duration.ofHours(1));
        ttls.put("userDataScope", Duration.ofHours(1));
        ttls.put("userMaxRoleLevel", Duration.ofHours(1));
        ttls.put("roleLevel", Duration.ofHours(2));
        ttls.put("permissionTree", Duration.ofHours(2));
        ttls.put("permissionMapping", Duration.ofMinutes(5));
        ttls.put("roles", Duration.ofHours(1));
        ttls.put("role", Duration.ofHours(1));
        ttls.put("rolePermissions", Duration.ofHours(1));
        ttls.put("apiPermissions", Duration.ofHours(2));

        // 部门相关缓存
        ttls.put("userDeptId", Duration.ofMinutes(30));
        ttls.put("deptPath", Duration.ofHours(2));
        ttls.put("deptTree", Duration.ofHours(1));
        ttls.put("deptChildren", Duration.ofHours(1));
        ttls.put("accessibleDeptIds", Duration.ofHours(1));

        // 临时角色缓存
        ttls.put("userTemporaryRoles", Duration.ofMinutes(15));

        long localMaxSize = 10_000L;
        return new TwoLevelCacheManager(redisTemplate, defaultTtl, ttls, localMaxSize);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1)); // 默认缓存1小时

        // 为不同的缓存设置不同的过期时�?
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("user", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(30))); // 用户缓存30分钟

        cacheConfigurations.put("userInfo", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(30))); // 用户信息缓存30分钟

        cacheConfigurations.put("userRoles", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 用户角色缓存1小时

        cacheConfigurations.put("userPermissions", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 用户权限缓存1小时

        cacheConfigurations.put("permissionTree", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(2))); // 权限树缓�?小时

        cacheConfigurations.put("roles", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 角色列表缓存1小时

        cacheConfigurations.put("role", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 角色缓存1小时

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisSerializer<Object> jackson2JsonRedisSerializer() {
        return RedisSerializer.json();
    }
}
