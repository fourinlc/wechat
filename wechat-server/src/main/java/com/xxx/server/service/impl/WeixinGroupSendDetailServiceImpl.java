package com.xxx.server.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.exception.BusinessException;
import com.xxx.server.mapper.WeixinGroupSendDetailMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 批量拉群
 * </p>
 *
 * @author lc
 * @since 2022-08-29
 */
@Service
@Slf4j
public class WeixinGroupSendDetailServiceImpl extends ServiceImpl<WeixinGroupSendDetailMapper, WeixinGroupSendDetail> implements IWeixinGroupSendDetailService {

    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;

    @Resource
    private IWeixinDictionaryService weixinDictionaryService;

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Resource
    private IWeixinGroupSendDetailService weixinGroupSendDetailService;

    @Resource
    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @Resource
    private DelayMqProducer delayMqProducer;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.groupSend}")
    private String groupSendTag;

    public JSONObject groupSendDetail(List<String> chatRoomIds, String masterWxId, List<String> slaveWxIds, boolean flag, Date fixedTime) {
        WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(masterWxId);
        Assert.isTrue(weixinBaseInfo != null && StrUtil.equals("1", weixinBaseInfo.getState()), "主账号已不在线");
        // 校验slaveWxIdA slaveWxIdB是否正常状态，查看是否需要自动进群
        Assert.isTrue(slaveWxIds.size() > 0, "请至少选择一个邀请号码");
        List<WeixinBaseInfo> weixinBaseInfoList = weixinBaseInfoService.listByIds(slaveWxIds);
        if (flag) {
            // 校验子账号是否在线状态，如果不在线直接抛出异常
            slaveWxIds = weixinBaseInfoList.stream()
                    .filter(weixinBaseInfo1 -> StrUtil.equals("1", weixinBaseInfo1.getState()))
                    .map(WeixinBaseInfo::getWxId)
                    .collect(Collectors.toList());
            Assert.isTrue(slaveWxIds.size() > 0, "请至少选择一个在线的邀请号码");
        }
        JSONObject result = JSONObject.of("code", 200, "msg", "发送消息成功");
        WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
        WeixinAsyncEventCall old = weixinAsyncEventCallService.getOne(
                Wrappers.lambdaQuery(WeixinAsyncEventCall.class)
                        .eq(WeixinAsyncEventCall::getWxId, masterWxId)
                        .eq(WeixinAsyncEventCall::getEventType, ResConstant.ASYNC_EVENT_GROUP_SEND)
                        // 获取正在处理的该微信数据
                        .eq(WeixinAsyncEventCall::getResultCode, "99"));
        Date delay = new Date();
        if (old != null) {
            log.info("校验上次微信执行是否完成");
            LocalDateTime planTime = old.getPlanTime();
            if (planTime == null) {
                // 结束上次异常群聊
                weixinAsyncEventCallService.updateById(old.setResult("系统异常").setResultCode(500));
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_SEND)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(masterWxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            } else if (planTime.compareTo(LocalDateTime.now()) > 0) {
                // 直接提醒还存在待完成的数据，返回开始预计开始时间和预计完成时间
                log.info("该微信上次群聊还没执行完成：{}", masterWxId);
                result.put("code", 500);
                result.put("planTime", weixinAsyncEventCall.getPlanTime());
                result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
                result.put("msg", "该微信上次群聊还没执行完成");
                result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
                return result;
            } else {
                // 更新这条异常数据
                // 如果计划完成时间小于当前完成时间，直接将该计划停止，并标明原因
                weixinAsyncEventCallService.updateById(old.setResult("系统异常").setResultCode(500));
                if (fixedTime != null) {
                    if (fixedTime.compareTo(new Date()) > 0) {
                        delay = fixedTime;
                    }
                }
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_SEND)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(masterWxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            }
        } else {
            // 该微信首次创建话术，直接延时至指定时间
            if (fixedTime != null) {
                if (fixedTime.compareTo(new Date()) > 0) {
                    delay = fixedTime;
                }
            }
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_GROUP_SEND)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(masterWxId)
                    .setPlanStartTime(LocalDateTimeUtil.of(delay))
                    // 设置99为处理中状态
                    .setResultCode(99);
            weixinAsyncEventCallService.save(weixinAsyncEventCall);
        }
        // 组装消息体
        if (weixinAsyncEventCall.getPlanTime() != null) {
            // 重置老数据直接添加至队尾
            delay = DateUtil.date(weixinAsyncEventCall.getPlanTime());
        }
        // 开始组装发送信息
        List<WeixinDictionary> weixinDictionaries = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("groupSend"));
        // 获取对应随机数字1-5, 默认2-4秒
        JSONObject dices = new JSONObject();
        weixinDictionaries.forEach(scanIntoUrlGroupTime -> {
            dices.put(scanIntoUrlGroupTime.getDicKey(), scanIntoUrlGroupTime.getDicValue());
        });
        // 进群间隔时间
        int max = dices.getIntValue("max", 15);
        int min = dices.getIntValue("min", 10);
        int sheaves = dices.getIntValue("sheaves", 2);
        int rate = dices.getIntValue("rate", 10);
        int between = dices.getIntValue("between", 1);
        // 开始循环进群操作
        log.info("拉群配置信息{}", dices);
        for (int i = 0; i < chatRoomIds.size(); i++) {
            if ((i + 1) % (sheaves * rate) == 0) {
                // log.info("新的一轮操作：{}", i);
                // 说明一轮数据完成，增加间隔时间
                delay = DateUtils.addSeconds(delay, between * 60);
            }
            String chatRoomId = chatRoomIds.get(i);
            // 获取对应的key值信息
            JSONObject msg = JSONObject.of("slaveWxIds", slaveWxIds,
                    "wxId", masterWxId,
                    "asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId()
            );
            msg.put("chatRoomId", chatRoomId);
            msg.put("flag", flag);

            // 设置随机时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, min, max);
            // 异步更新返回成功时或者失败时更新群链接状态
            try {
                // 缓存中获取进群间隔时间，分发mq延时任务进行消费
                // 更新消息处理状态为消息正在处理中
                WeixinGroupSendDetail weixinGroupSendDetail = new WeixinGroupSendDetail()
                        .setMasterWxId(masterWxId)
                        .setAsyncEventCallId(weixinAsyncEventCall.getAsyncEventCallId())
                        .setCreateTime(LocalDateTime.now().toLocalDate())
                        .setChatRoomId(chatRoomId)
                        .setStatus("99");
                save(weixinGroupSendDetail);
                msg.put("groupSendDetailId", weixinGroupSendDetail.getGroupSendDetailId());
                Message message = new Message(consumerTopic, groupSendTag, JSON.toJSONBytes(msg));
                log.info("发送延时消息延时时间为：{}", delay);
                delayMqProducer.sendDelay(message, delay);
                // 设置状态为等待处理中
            } catch (Exception e) {
                log.error("消息处理失败:{}", e.getMessage());
                // 重置回调参数异步回调参数，更新为处理失败
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("mq发送消息失败"));
                // baseMapper.updateById(weixinGroupSendDetail.setStatus("5"));
                throw new BusinessException("mq消息发送失败，请检测网络情况");
            }
        }
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
        result.put("planTime", weixinAsyncEventCall.getPlanTime());
        return result;
    }

    // 批量拉群实时列表展示
    public JSONArray queryList(Long asyncEventCallId){
        // 查询当前微信是否存在拉群操作
        WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
        Assert.notNull(weixinAsyncEventCall, "批次号不存在");
        // 获取该批次所有的列表
        JSONArray jsonArray = JSONArray.of();
        List<WeixinGroupSendDetail> weixinGroupSendDetails = weixinGroupSendDetailService.list(Wrappers.lambdaQuery(WeixinGroupSendDetail.class).eq(WeixinGroupSendDetail::getAsyncEventCallId, weixinAsyncEventCall.getAsyncEventCallId()));
        for (WeixinGroupSendDetail weixinGroupSendDetail : weixinGroupSendDetails) {
            // 组装子号成功失败信息
            Long groupSendDetailId = weixinGroupSendDetail.getGroupSendDetailId();
            List<WeixinGroupLinkDetail> weixinGroupLinkDetails = weixinGroupLinkDetailService.list(Wrappers.lambdaQuery(WeixinGroupLinkDetail.class).eq(WeixinGroupLinkDetail::getGroupSendDetailId, groupSendDetailId));
            JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(weixinGroupSendDetail));
            if(weixinGroupLinkDetails.size() > 0){
                // 增加子号信息基本信息至数据单条数据,提取被邀请人昵称以及处理状态
                List<JSONObject> collect = weixinGroupLinkDetails
                        .stream()
                        .map(weixinGroupLinkDetail -> JSONObject.of("linkStatus", weixinGroupLinkDetail.getLinkStatus(), "toUserName", weixinGroupLinkDetail.getToUserName()))
                        .collect(Collectors.toList());
                jsonObject.put("child", collect);
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

}
