package com.xxx.server.mq;

import com.alibaba.fastjson2.JSONObject;

/**
 * 业务消息处理基类
 */
public interface MqMessageHandler {

    default boolean process(JSONObject message){
        return true;
    }
}
