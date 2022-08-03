package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinGroupLinkDetailMapper;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinDictionaryService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  微信群邀请相关
 * </p>
 *
 * @author qj
 * @since 2022-07-16
 */
@Service
@Slf4j
public class WeixinGroupLinkDetailServiceImpl extends ServiceImpl<WeixinGroupLinkDetailMapper, WeixinGroupLinkDetail> implements IWeixinGroupLinkDetailService {

    @Resource
    private IWeixinDictionaryService weixinDictionaryService;

    @Resource
    private DelayMqProducer delayMqProducer;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.qunGroup}")
    private String consumerQunGroupTag;

    public boolean batchScanIntoUrlGroup(List<Long> linkIds){
        List<WeixinGroupLinkDetail> weixinGroupLinkDetails = baseMapper.selectBatchIds(linkIds);
        // 遍历进群
        List<WeixinDictionary> scanIntoUrlGroupTimes = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("qun").setDicKey("scanIntoUrlGroupTime"));
        Assert.isTrue(scanIntoUrlGroupTimes.size() == 1, "系统进群消息配置异常");
        String scanIntoUrlGroupTime = scanIntoUrlGroupTimes.get(0).getDicValue();
        Date delay = new Date();
        for (WeixinGroupLinkDetail weixinGroupLinkDetail : weixinGroupLinkDetails) {
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
            // 获取对应的key值信息
            multiValueMap.add("key", weixinGroupLinkDetail.getKey());
            JSONObject jsonObject = JSONObject.of("Url", weixinGroupLinkDetail.getContent());
            JSONObject msg = JSONObject.of("param", jsonObject, "query", multiValueMap, "code", WechatApiHelper.SCAN_INTO_URL_GROUP.getCode());
            //TODO 是否有必要设置成异步消息,加快响应时间
            Message message = new Message(consumerTopic, consumerQunGroupTag, JSON.toJSONBytes(msg));
            delay = DateUtils.addSeconds(delay, Integer.parseInt(scanIntoUrlGroupTime));
            // 异步更新返回成功时或者失败时更新群链接状态
            try {
                // 缓存中获取进群间隔时间，分发mq延时任务进行消费
                delayMqProducer.sendDelay(message, delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.info("消息处理失败");
                return false;
            }
        }
        return true;
    }
}
