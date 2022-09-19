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
import com.xxx.server.exception.BusinessException;
import com.xxx.server.mapper.WeixinGroupLinkDetailMapper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinDictionaryService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 微信群邀请相关
 * </p>
 *
 * @author qj
 * @since 2022-07-16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeixinGroupLinkDetailServiceImpl extends ServiceImpl<WeixinGroupLinkDetailMapper, WeixinGroupLinkDetail> implements IWeixinGroupLinkDetailService {

    private final IWeixinDictionaryService weixinDictionaryService;

    private final DelayMqProducer delayMqProducer;

    private final IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.qunGroupNew}")
    private String consumerQunGroupTag;

    private static final String COMPANY_STATUS = "openim";

    public JSONObject batchScanIntoUrlGroup(List<Long> linkIds, List<String> wxIds, String wxId) {
        Date delay = new Date();
        //TODO 校验好友关系
        JSONObject result = JSONObject.of("code", 200, "msg", "链接进群已发送");
        WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
        WeixinAsyncEventCall old = weixinAsyncEventCallService.getOne(
                Wrappers.lambdaQuery(WeixinAsyncEventCall.class)
                        .eq(WeixinAsyncEventCall::getWxId, wxId)
                        // 获取正在处理的该微信数据
                        .eq(WeixinAsyncEventCall::getResultCode, "99"));
        if (old != null) {
            log.info("上次微信执行未完成");
            LocalDateTime planTime = old.getPlanTime();
            if (planTime == null) {
                log.info("上次微信执行计划完成时间为空");
                // 结束上次异常群聊
                weixinAsyncEventCallService.updateById(old.setResult("系统异常").setResultCode(500));
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_SEND)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(wxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            } else if (planTime.compareTo(LocalDateTime.now()) > 0) {
                // 直接提醒还存在待完成的数据，返回开始预计开始时间和预计完成时间
                log.info("该微信上次链接进群还没执行完成：{}", wxId);
                result.put("code", 500);
                result.put("planTime", weixinAsyncEventCall.getPlanTime());
                result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
                result.put("msg", "该微信上次链接进群还没执行完成");
                result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
                return result;
            } else {
                log.info("上次微信执行计划已完成，更新上次执行时间");
                // 更新这条异常数据
                // 如果计划完成时间小于当前完成时间，直接将该计划停止，并标明原因
                weixinAsyncEventCallService.updateById(old.setResult("系统异常").setResultCode(500));
                /*if (fixedTime != null) {
                    if (fixedTime.compareTo(new Date()) > 0) {
                        delay = fixedTime;
                    }
                }*/
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_SEND)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(wxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            }
        }else {
            log.info("首次微信执行计划，生成新的批次信息");
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_SCAN_INTO_URL_GROUP)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(wxId)
                    // 设置99为处理中状态
                    .setResultCode(99);
            weixinAsyncEventCallService.save(weixinAsyncEventCall);
        }
        // 增加批次号入参
        result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
        List<WeixinDictionary> scanIntoUrlGroupTimes = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("scanIntoUrlGroupTime"));
        // Assert.isTrue(scanIntoUrlGroupTimes.size() >= 2, "系统进群消息配置异常");
        // 获取对应随机数字1-5, 默认2-4秒
        JSONObject dices = new JSONObject();
        scanIntoUrlGroupTimes.forEach(scanIntoUrlGroupTime -> {
            dices.put(scanIntoUrlGroupTime.getDicKey(), scanIntoUrlGroupTime.getDicValue());
        });
        int max = dices.getIntValue("max", 6);
        int min = dices.getIntValue("min", 4);
        int sheaves = dices.getIntValue("sheaves", 2);
        int rate = dices.getIntValue("rate", 10);
        int between = dices.getIntValue("between", 1);
        log.info("群邀请配置信息：{}", dices);
        // 获取群链接列表
        long l = System.currentTimeMillis();
        List<WeixinGroupLinkDetail> weixinGroupLinkDetailList = listByIds(linkIds);
        for (int i = 0; i < weixinGroupLinkDetailList.size(); i++) {
            if ((i + 1) % (sheaves * rate +1) == 0) {
                // log.info("新的一轮操作：{}", i);
                // 说明一轮数据完成，增加间隔时间
                delay = DateUtils.addSeconds(delay, between * 60);
            }
            WeixinGroupLinkDetail weixinGroupLinkDetail = weixinGroupLinkDetailList.get(i);
            // 获取对应的key值信息
            JSONObject msg = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(),
                    "linkId", weixinGroupLinkDetail.getLinkId(),
                    "wxIds", wxIds);
            msg.put("count", weixinGroupLinkDetailList.size());
            // 增加时间戳入参
            msg.put("current", l);
            msg.put("wxId", wxId);
            Message message = new Message(consumerTopic, consumerQunGroupTag, JSON.toJSONBytes(msg));
            // 异步更新返回成功时或者失败时更新群链接状态
            // 设置随机时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, min, max);
            try {
                // 缓存中获取进群间隔时间，分发mq延时任务进行消费
                log.info("发送延时消息延时时间为：{}", delay);
                delayMqProducer.sendDelay(message, delay);
                // 更新消息处理状态为消息正在处理中
                baseMapper.updateById(weixinGroupLinkDetail.setLinkStatus("99"));
                // 设置状态为等待处理中
            } catch (Exception e) {
                log.error("消息处理失败:{}", e.getMessage());
                // 重置回调参数异步回调参数，更新为处理失败
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("mq发送消息失败"));
                baseMapper.updateById(weixinGroupLinkDetail.setLinkStatus("500"));
                throw new BusinessException("mq消息发送失败，请检测网络情况");
            }
        }
        // 后边加入的微信进群操作需要
        // 增加最大操作延时时间
        // delay = DateUtils.addSeconds(delay, max * 2);
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
        // 增加默认执行时间设置
        log.info("生成批次号情况：{}", weixinAsyncEventCall);
        result.put("planTime", weixinAsyncEventCall.getPlanTime());
        return result;
    }

    @Override
    public boolean saveBatch(List<WeixinGroupLinkDetail> weixinGroupLinkDetails) {
        // 去重，剔除
        log.info("开始保存群聊以及更新文本信息：{}", weixinGroupLinkDetails);
        for (WeixinGroupLinkDetail weixinGroupLinkDetail : weixinGroupLinkDetails) {
            if (Objects.isNull(weixinGroupLinkDetail.getThumbUrl())) {
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
            weixinGroupLinkDetail.setCompanyStatus(StrUtil.contains(content, COMPANY_STATUS) ? "1" : "0");
            baseMapper.insert(weixinGroupLinkDetail);
        }
        // 获取对应文本信息的数据
        List<WeixinGroupLinkDetail> groupLinkDetails = weixinGroupLinkDetails.stream().filter(weixinGroupLinkDetail -> weixinGroupLinkDetail.getMsgType() == 1).collect(Collectors.toList());
        // 更新对应的wxId列表remark字段
        for (WeixinGroupLinkDetail groupLinkDetail : groupLinkDetails) {
            // 获取单个好友当天群聊信息
            List<WeixinGroupLinkDetail> weixinGroupLinkDetailList = list(Wrappers.lambdaQuery(WeixinGroupLinkDetail.class)
                    .eq(WeixinGroupLinkDetail::getToUserWxId, groupLinkDetail.getToUserWxId())
                    .eq(WeixinGroupLinkDetail::getFromUserWxId, groupLinkDetail.getFromUserWxId())
                    .isNull(WeixinGroupLinkDetail::getRemark));
            // 批量更新这些列表remark状态
            weixinGroupLinkDetailList.forEach(weixinGroupLinkDetail -> weixinGroupLinkDetail.setRemark(groupLinkDetail.getContent()));
            updateBatchById(weixinGroupLinkDetailList);
        }
        return true;
    }

    public Object query(WeixinGroupLinkDetail weixinGroupLinkDetail, List<String> linkStatus) {
        // 填充之邀请链接中
        return list(Wrappers.<WeixinGroupLinkDetail>lambdaQuery()
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvitationTime()), WeixinGroupLinkDetail::getInvitationTime, weixinGroupLinkDetail.getInvitationTime())
                .like(StrUtil.isNotEmpty(weixinGroupLinkDetail.getFromUserName()), WeixinGroupLinkDetail::getFromUserName, weixinGroupLinkDetail.getFromUserName())
                .in(StrUtil.isNotEmpty(weixinGroupLinkDetail.getLinkStatus()), WeixinGroupLinkDetail::getLinkStatus, linkStatus)
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getToUserWxId()), WeixinGroupLinkDetail::getToUserWxId, weixinGroupLinkDetail.getToUserWxId())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvalidStatus()), WeixinGroupLinkDetail::getInvalidStatus, weixinGroupLinkDetail.getInvalidStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getVerifyStatus()), WeixinGroupLinkDetail::getVerifyStatus, weixinGroupLinkDetail.getVerifyStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getRepeatStatus()), WeixinGroupLinkDetail::getRepeatStatus, weixinGroupLinkDetail.getRepeatStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getCompanyStatus()), WeixinGroupLinkDetail::getCompanyStatus, weixinGroupLinkDetail.getCompanyStatus())
                .eq(Objects.nonNull(weixinGroupLinkDetail.getAsyncEventCallId()), WeixinGroupLinkDetail::getAsyncEventCallId, weixinGroupLinkDetail.getAsyncEventCallId())
                .eq(Objects.nonNull(weixinGroupLinkDetail.getUpdateTime()), WeixinGroupLinkDetail::getUpdateTime, weixinGroupLinkDetail.getUpdateTime()));
    }

}