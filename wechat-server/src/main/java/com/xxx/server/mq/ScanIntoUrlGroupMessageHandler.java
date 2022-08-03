package com.xxx.server.mq;

import com.alibaba.fastjson2.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.xxx.server.enums.WechatApiHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 进群成功失败信息处理，tag为scanIntoUrlGroup处理类
 */
@Component("scanIntoUrlGroup")
@Slf4j
public class ScanIntoUrlGroupMessageHandler implements MqMessageHandler{

    @Override
    public boolean process(JSONObject message) {
        log.info("进群消息处理====》{}", message);
        String code = message.getString("code");
        WechatApiHelper wechatApiHelper = WechatApiHelper.getWechatApiHelper(code);
        if(wechatApiHelper == null){
            log.info("群消息体数据异常,跳过处理：{}", message);
            return true;
        }
        LinkedTreeMap query = (LinkedTreeMap)message.get("query");
        JSONObject param = message.getJSONObject("param");
        MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap(query);
        Object data = wechatApiHelper.invoke(param, multiValueMap);
        // TODO 具体更新操作逻辑
        return true;
    }
}
