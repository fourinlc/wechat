package com.xxx.server.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
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
        // 遍历进群,增加参数配置，增加进群个数后休息时间配置
        List<WeixinDictionary> scanIntoUrlGroupTimes = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("scanIntoUrlGroupTime"));
        // Assert.isTrue(scanIntoUrlGroupTimes.size() >= 2, "系统进群消息配置异常");
        // 获取对应随机数字1-5, 默认2-4秒
        JSONObject dices = new JSONObject();
        scanIntoUrlGroupTimes.forEach(scanIntoUrlGroupTime->{
            dices.put(scanIntoUrlGroupTime.getDicKey(), scanIntoUrlGroupTime.getDicValue());
        });
        int max = dices.getIntValue("max", 4);
        int min = dices.getIntValue("min", 2);
        int sheaves = dices.getIntValue("sheaves", 2);
        int rate = dices.getIntValue("rate", 1);
        int between = dices.getIntValue("between", 1);
        log.info("群邀请配置信息：{}", dices);
        Date delay = new Date();
        for (int i = 0; i < weixinGroupLinkDetails.size(); i++) {
            if ((i + 1) % (sheaves * rate) == 0) {
                // log.info("新的一轮操作：{}", i);
                // 说明一轮数据完成，增加间隔时间
                delay = DateUtils.addSeconds(delay, between * 60);
            }
            WeixinGroupLinkDetail weixinGroupLinkDetail = weixinGroupLinkDetails.get(i);
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
            // 获取对应的key值信息
            multiValueMap.add("key", weixinGroupLinkDetail.getKey());
            JSONObject jsonObject = JSONObject.of("Url", weixinGroupLinkDetail.getContent());
            JSONObject msg = JSONObject.of("param", jsonObject, "query", multiValueMap, "code", WechatApiHelper.SCAN_INTO_URL_GROUP.getCode());
            Message message = new Message(consumerTopic, consumerQunGroupTag, JSON.toJSONBytes(msg));
            // 设置随机时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, min, max);
            // 异步更新返回成功时或者失败时更新群链接状态
            try {
                // 缓存中获取进群间隔时间，分发mq延时任务进行消费
                log.info("发送延时消息延时时间为：{}", delay);
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
