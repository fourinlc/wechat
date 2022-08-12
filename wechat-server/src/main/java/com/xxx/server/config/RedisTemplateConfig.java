package com.xxx.server.config;

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisTemplateConfig extends CachingConfigurerSupport {
 /*   @Bean
    public CacheManager cacheManager(RedisTemplate<?, ?> redisTemplate) {
        CacheManager cacheManager = new RedisCacheManager(redisTemplate);
        return cacheManager;
    }*/
    // 以下两种redisTemplate自由根据场景选择
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 采用fastjson序列化
        template.setDefaultSerializer(new GenericFastJsonRedisSerializer());
        //key采用string序列化配置
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new GenericFastJsonRedisSerializer());
        //value采用jackson序列化配置
        template.setValueSerializer(new GenericFastJsonRedisSerializer());
        return template;
    }
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(factory);
        return stringRedisTemplate;
    }

    /**
     * 修改 Cacheable 默认序列化方式 使用Redis配置的序列化
     * 解决 @Cacheable 序列化失败 而 RedisUtil可以成功 问题
     * @param redisTemplate RedisTemplate
     * @return RedisCacheManager
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisTemplate redisTemplate) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisTemplate.getConnectionFactory());
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                // 设置默认的超时时间为2小时
                .entryTtl(Duration.ofHours(2))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisTemplate.getValueSerializer()))
                // 设置默认的缓存前缀
                .prefixCacheNameWith("CACHE_");
        return new RedisCacheManager(redisCacheWriter, redisCacheConfiguration);
    }
}