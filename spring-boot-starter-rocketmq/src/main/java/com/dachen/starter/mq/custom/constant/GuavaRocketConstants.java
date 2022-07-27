package com.dachen.starter.mq.custom.constant;

/**
 * @author : Guava
 * @version 1.0
 * @projectName：civism-rocket
 * @className：GuavaRocketConstants
 * @date 2020/1/9 1:42 下午
 * @return
 */
public class GuavaRocketConstants {
    /**
     * 时间
     */
    public static final String GUAVA_TIMES          = "times";

    /**
     * 原始的topic
     */
    public static final String GUAVA_ORIGINAL_TOPIC = "guava_original_topic";

    /**
     * 原始的tag
     */
    public static final String GUAVA_ORIGINAL_TAG   = "guava_original_tag";

    /**
     * 原始的key
     */
    public static final String GUAVA_ORIGINAL_KEY   = "guava_original_key";

    /**
     * 唯一ID
     */
    public static final String GUAVA_ORIGINAL_UUID  = "guava_original_UUID";

    /**
     * 代理的topic
     */
    public static final String PROXY_TOPIC          = "guava_topic";

    /**
     * 最后时间直接不走代理topic,定时直接处理
     */
    public static final Long   TIME_OUT             = 10L;
}
