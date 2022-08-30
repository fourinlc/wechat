package com.xxx.server.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.pojo.WeixinLog;
import com.xxx.server.service.IWeixinLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("wechatLogTag")
@Slf4j
@AllArgsConstructor
public class WechatLogMqMessageHandler implements MqMessageHandler{

    private IWeixinLogService weixinLogService;

    @Override
    public boolean process(JSONObject message) {
        // 异步保存微信操作日志信息,不管成功与否，不再操作
        try {
            JSONObject result = message.getJSONObject("result");
            String code = result.getString(ResConstant.CODE);
            WeixinLog weixinLog = message.to(WeixinLog.class);
            weixinLogService.save(weixinLog.setStatus(code));
        }catch (Exception e){
            log.error("保存操作日志异常：{}", ExceptionUtil.getMessage(e));
        }
        return true;
    }
}
