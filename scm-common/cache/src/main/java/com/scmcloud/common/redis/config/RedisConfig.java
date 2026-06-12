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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import com.scmcloud.common.cache.spring.TwoLevelCacheManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 閰嶇疆锟?
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

        // key 閲囩敤String鐨勫簭鍒楀寲鏂瑰紡
        template.setKeySerializer(stringSerializer);
        // hash 鐨刱ey涔熼噰鐢⊿tring鐨勫簭鍒楀寲鏂瑰紡
        template.setHashKeySerializer(stringSerializer);
        // value 搴忓垪鍖栨柟寮忛噰鐢╦ackson
        template.setValueSerializer(serializer);
        // hash  鐨剉alue搴忓垪鍖栨柟寮忛噰鐢╦ackson
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "scm.cache.two-level.enabled", havingValue = "true", matchIfMissing = false)
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
    @ConditionalOnProperty(name = "scm.cache.two-level.enabled", havingValue = "true", matchIfMissing = false)
    public CacheManager twoLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
        Duration defaultTtl = Duration.ofHours(1);
        Map<String, Duration> ttls = new HashMap<>();
        // 鐢ㄦ埛鍩烘湰淇℃伅缂撳瓨
        ttls.put("user", Duration.ofMinutes(30));
        ttls.put("userInfo", Duration.ofMinutes(30));
        ttls.put("userDetails", Duration.ofMinutes(30));

        // 鏉冮檺鍜岃鑹茬紦锟?
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

        // 閮ㄩ棬鐩稿叧缂撳瓨
        ttls.put("userDeptId", Duration.ofMinutes(30));
        ttls.put("deptPath", Duration.ofHours(2));
        ttls.put("deptTree", Duration.ofHours(1));
        ttls.put("deptChildren", Duration.ofHours(1));
        ttls.put("accessibleDeptIds", Duration.ofHours(1));

        // 涓存椂瑙掕壊缂撳瓨
        ttls.put("userTemporaryRoles", Duration.ofMinutes(15));

        long localMaxSize = 10_000L;
        return new TwoLevelCacheManager(redisTemplate, defaultTtl, ttls, localMaxSize);
    }

    @Bean
    @ConditionalOnMissingBean(TwoLevelCacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1)); // 榛樿缂撳瓨1灏忔椂

        // 涓轰笉鍚岀殑缂撳瓨璁剧疆涓嶅悓鐨勮繃鏈熸椂锟?
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("user", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(30))); // 鐢ㄦ埛缂撳瓨30鍒嗛挓

        cacheConfigurations.put("userInfo", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(30))); // 鐢ㄦ埛淇℃伅缂撳瓨30鍒嗛挓

        cacheConfigurations.put("userRoles", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 鐢ㄦ埛瑙掕壊缂撳瓨1灏忔椂

        cacheConfigurations.put("userPermissions", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 鐢ㄦ埛鏉冮檺缂撳瓨1灏忔椂

        cacheConfigurations.put("permissionTree", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(2))); // 鏉冮檺鏍戠紦锟藉皬鏃?

        cacheConfigurations.put("roles", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 瑙掕壊鍒楄〃缂撳瓨1灏忔椂

        cacheConfigurations.put("role", RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofHours(1))); // 瑙掕壊缂撳瓨1灏忔椂

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisSerializer<Object> jackson2JsonRedisSerializer() {
        return RedisSerializer.json();
    }
}
