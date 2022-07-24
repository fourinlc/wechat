package com.xxx.server.sched;

import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 异步处理群链接信息
 */
/*@Component*/
@AllArgsConstructor
@Slf4j
public class WeixinGroupLink implements CommandLineRunner  {

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @Override
    public void run(String... args) throws Exception {
        for(;;){
            // 循环处理主号接收到的微信群链接邀请，设置间隔时间
            // 应该是遍历所有在线主号信息
            MultiValueMap<String,String> query = new LinkedMultiValueMap<>();
            // 暂时写死单个微信信息
            query.set("key", "de16191a-c633-418f-9458-a9af51b99d0e");
            // 短链接获取最新好友消息，用于筛选群链接信息
            Object object = WechatApiHelper.NEW_SYNC_HISTORY_MESSAGE.invoke(JSONObject.of("Scene", 1), query);
            // 解析具体微信消息,拿到具体群链接信息并入库
            if ((object instanceof JSONObject)) {

            }
            Thread.sleep(2000);


        }
    }
}
