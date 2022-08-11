package com.xxx.server.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinGroupLinkDetailMapper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinDictionaryService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.qunGroup}")
    private String consumerQunGroupTag;

    private IWeixinBaseInfoService weixinBaseInfoService;

    private static final String COMPANY_STATUS = "openim";

    public boolean batchScanIntoUrlGroup(List<Long> linkIds){
        // 暂时设置为每一个微信为一组独立的队列，每次处理生成一个特定的批次号，用于统一处理异常或者终止后续操作，减少同一时间内操作
        List<WeixinGroupLinkDetail> weixinGroupLinkDetailsVo = baseMapper.selectBatchIds(linkIds);
        // 依照微信id分组并生成对应的批次号
        Map<String, List<WeixinGroupLinkDetail>> maps = weixinGroupLinkDetailsVo.stream().collect(Collectors.groupingBy(WeixinGroupLinkDetail::getToUserWxId));
        maps.forEach((wxId, weixinGroupLinkDetailList)->{
            // 校验是否存在子号
            /*List<WeixinBaseInfo> weixinBaseInfoList = weixinBaseInfoService.list(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getParentWxid, wxId));
            Assert.isTrue(weixinBaseInfoList.size() > 0, "请先关联对应的子账号wxId ：" + wxId);*/
            // 可以使用多线程处理每一个线程处理一个wxId数据即可
            //TODO 查看该微信下是否存在
            // 直接生成的批次号，用于错误时回调
            // 遍历进群,增加参数配置，增加进群个数后休息时间配置
            WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_SCAN_INTO_URL_GROUP)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(wxId)
                    // 设置99为处理中状态
                    .setResultCode(99);
            weixinAsyncEventCallService.save(weixinAsyncEventCall);
            List<WeixinDictionary> scanIntoUrlGroupTimes = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("scanIntoUrlGroupTime"));
            // Assert.isTrue(scanIntoUrlGroupTimes.size() >= 2, "系统进群消息配置异常");
            // 获取对应随机数字1-5, 默认2-4秒
            JSONObject dices = new JSONObject();
            scanIntoUrlGroupTimes.forEach(scanIntoUrlGroupTime->{
                dices.put(scanIntoUrlGroupTime.getDicKey(), scanIntoUrlGroupTime.getDicValue());
            });
            int max = dices.getIntValue("max", 6);
            int min = dices.getIntValue("min", 4);
            int sheaves = dices.getIntValue("sheaves", 2);
            int rate = dices.getIntValue("rate", 10);
            int between = dices.getIntValue("between", 1);
            log.info("群邀请配置信息：{}", dices);
            Date delay = new Date();
            for (int i = 0; i < weixinGroupLinkDetailList.size(); i++) {
                if ((i + 1) % (sheaves * rate) == 0) {
                    // log.info("新的一轮操作：{}", i);
                    // 说明一轮数据完成，增加间隔时间
                    delay = DateUtils.addSeconds(delay, between * 60);
                }
                WeixinGroupLinkDetail weixinGroupLinkDetail = weixinGroupLinkDetailList.get(i);
                MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
                // 获取对应的key值信息
                multiValueMap.add("key", weixinGroupLinkDetail.getKey());
                JSONObject jsonObject = JSONObject.of("Url", weixinGroupLinkDetail.getContent());
                JSONObject msg = JSONObject.of("param", jsonObject,
                        "query", multiValueMap,
                        "code", WechatApiHelper.SCAN_INTO_URL_GROUP.getCode()
                        );
                msg.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
                msg.put("linkId", weixinGroupLinkDetail.getLinkId());
                Message message = new Message(consumerTopic, consumerQunGroupTag, JSON.toJSONBytes(msg));
                // 设置随机时间
                delay = RandomUtil.randomDate(delay, DateField.SECOND, min, max);
                // 异步更新返回成功时或者失败时更新群链接状态
                try {
                    // 缓存中获取进群间隔时间，分发mq延时任务进行消费
                    log.info("发送延时消息延时时间为：{}", delay);
                    delayMqProducer.sendDelay(message, delay);
                    // 更新消息处理状态为消息正在处理中
                    baseMapper.updateById(weixinGroupLinkDetail.setLinkStatus("99"));
                    // 设置状态为等待处理中
                } catch (InterruptedException e) {
                    log.info("消息处理失败");
                    // return false;
                }
            }
            // 设置预期完成时间
            weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay));
            // 后边加入的微信进群操作需要
            weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
        });
        return true;
    }

    @Override
    public boolean saveBatch(List<WeixinGroupLinkDetail> weixinGroupLinkDetails){
        for (WeixinGroupLinkDetail weixinGroupLinkDetail : weixinGroupLinkDetails) {
            if(Objects.isNull(weixinGroupLinkDetail.getThumbUrl())){
                continue;
            }
            //TODO step 1 首先本地去重,根据数据量再考虑是否采用接入redis布隆过滤器功能
            Integer count = baseMapper.selectCount(Wrappers.lambdaQuery(WeixinGroupLinkDetail.class).eq(WeixinGroupLinkDetail::getThumbUrl, weixinGroupLinkDetail.getThumbUrl()));
            weixinGroupLinkDetail.setRepeatStatus(count > 0 ? "1" : "0");
            // step 2 失效状态校验
            // 创建链接时间是否超过十四天，邀请人是否还是微信好友，群状态是否还是正常状态
            long time = DateUtil.between(new Date(), DateUtil.date(weixinGroupLinkDetail.getCreateTime() * 1000), DateUnit.SECOND, true);
            weixinGroupLinkDetail.setInvalidStatus(time > 14 * 24 * 60 * 60 ? "1" : "0");
            // step 3 企业微信校验
            String content = weixinGroupLinkDetail.getContent();
            weixinGroupLinkDetail.setCompanyStatus(StrUtil.contains(content, COMPANY_STATUS) ? "1" :"0");
            baseMapper.insert(weixinGroupLinkDetail);
        }
        return true;
    }
}