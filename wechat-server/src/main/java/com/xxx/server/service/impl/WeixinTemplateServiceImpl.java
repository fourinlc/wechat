package com.xxx.server.service.impl;

import cn.hutool.core.date.DateField;
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
import com.xxx.server.mapper.WeixinTemplateMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 微信AB话术类
 * </p>
 *
 * @author xxx
 * @since 2022-07-16
 */
@Service
//@AllArgsConstructor
@Slf4j
public class WeixinTemplateServiceImpl extends ServiceImpl<WeixinTemplateMapper, WeixinTemplate> implements IWeixinTemplateService {

    @Resource
    private IWeixinFileService weixinFileService;

    @Resource
    private DelayMqProducer delayMqProducer;

    @Resource
    private IWeixinTemplateDetailService weixinTemplateDetailService;

    @Resource
    private IWeixinRelatedContactsService weixinRelatedContactsService;

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Resource
    private IWeixinTemplateSendDetailService weixinTemplateSendDetailService;

    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.groupChat}")
    private String groupChatTag;

    @Override
    public JSONObject groupChat(List<String> chatRoomNames, String wxId, List<Long> templateIds, Date fixedTime) {
        checkWxId(wxId);
        JSONObject result = JSONObject.of("code", 200, "msg", "发送消息成功");
        // 增加开始时间返回以及预计完成时间
        // 检查模板的准确性
        List<WeixinTemplate> weixinTemplates = list(Wrappers.lambdaQuery(WeixinTemplate.class).in(WeixinTemplate::getTemplateId, templateIds));
        Assert.isTrue(weixinTemplates.size() == templateIds.size(), "模板数据有误,必须包含单人和双人模板");
        // 获取单人和双人模板，作为统一入参
        Map<String, List<WeixinTemplate>> maps = weixinTemplates.stream().collect(Collectors.groupingBy(WeixinTemplate::getTemplateType));
        JSONObject templateIdVos = JSONObject.of();
        maps.forEach((templateType, weixinTemplateVos) -> {
            List<Long> ids = weixinTemplateVos.stream().map(WeixinTemplate::getTemplateId).collect(Collectors.toList());
            templateIdVos.put(templateType, ids);
        });
        Assert.isTrue(templateIdVos.size() == 2, "缺少模板类型");
        // 查询其对应的子号信息
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(wxId);
        Assert.isTrue(StrUtil.isNotEmpty(weixinRelatedContacts.getRelated1()) && StrUtil.isNotEmpty(weixinRelatedContacts.getRelated2()), "请先关联好友再操作");
        // 构建回调参数，用于额外操作
        WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
        WeixinAsyncEventCall old = weixinAsyncEventCallService.getOne(
                Wrappers.lambdaQuery(WeixinAsyncEventCall.class)
                        .eq(WeixinAsyncEventCall::getWxId, wxId)
                        .eq(WeixinAsyncEventCall::getEventType, ResConstant.ASYNC_EVENT_GROUP_CHAT)
                        // 获取正在处理的该微信数据
                        .eq(WeixinAsyncEventCall::getResultCode, "99"));
        Date delay = new Date();
        if (old != null) {
            if (old.getPlanTime().compareTo(LocalDateTime.now()) > 0) {
                // 直接提醒还存在待完成的数据，返回开始预计开始时间和预计完成时间
                log.info("该微信上次群聊还没执行完成：{}", wxId);
                result.put("code", 500);
                result.put("planTime", weixinAsyncEventCall.getPlanTime());
                result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
                result.put("msg", "该微信上次群聊还没执行完成");
                result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
                return result;
                // weixinAsyncEventCall = old;
            } else {
                // 更新这条异常数据
                // 如果计划完成时间小于当前完成时间，直接将该计划停止，并标明原因
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResult("系统异常").setResultCode(500));
                if(fixedTime != null){
                    if (fixedTime.compareTo(new Date()) >  0) {
                        delay = fixedTime;
                    }
                }
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_CHAT)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(wxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            }
        } else {
            // 该微信首次创建话术，直接延时至指定时间
            if(fixedTime != null){
                if (fixedTime.compareTo(new Date()) >  0) {
                    delay = fixedTime;
                }
            }
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_GROUP_CHAT)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(wxId)
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
        for (String chatRoomName : chatRoomNames) {
            // 构建延时消息操作，暂时按照一个群5秒操作
            JSONObject jsonObject = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(),
                    "chatRoomName", chatRoomName,
                    "templateIds", templateIdVos);
            // 开始构建延时消息
            Message message = new Message(consumerTopic, groupChatTag, JSON.toJSONBytes(jsonObject));
            // 设置随机时间10-15秒执行时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, 32, 45);
            log.info("发送延时消息延时时间为：{}", delay);
            try {
                delayMqProducer.sendDelay(message, delay);
                // 记录已操作过的群聊信息，并标识为正在处理中
                WeixinTemplateSendDetail weixinTemplateSendDetail =
                        new WeixinTemplateSendDetail()
                                .setCreateTime(new Date())
                                .setWxId(wxId)
                                .setChatRoomId(chatRoomName)
                                .setStatus("99")
                                .setAsyncEventCallId(weixinAsyncEventCall.getAsyncEventCallId());
                // 先保存数据防止批量保存数据过多
                weixinTemplateSendDetailService.save(weixinTemplateSendDetail);
                // weixinTemplateSendDetails.add(weixinTemplateSendDetail);
            } catch (InterruptedException e) {
                log.error("mq发送延迟消息失败{}，wxId:{}", e.getMessage(), wxId);
                result.put("code", 500);
                result.put("msg", "mq发送延迟消息失败");
                // TODO 已发送出去消息还是正常处理，记录下发送成功的群id信息，防止二次发送
                return result;
            }
        }
        // 设置预期完成时间，用于后置添加进来的数据处理
        // 后边加入的微信进群操作需要
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
        result.put("planTime", weixinAsyncEventCall.getPlanTime());
        result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
        result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
        return result;
    }

    @Override
    public JSONObject groupChatNew(List<String> chatRoomNames, String wxIdA, String wxIdB, List<Long> templateIds, Date fixedTime) {
        // 校验两个小号是否在线
        checkId(wxIdA, wxIdB);
        JSONObject result = JSONObject.of("code", 200, "msg", "发送消息成功");
        // 增加开始时间返回以及预计完成时间
        // 检查模板的准确性
        List<WeixinTemplate> weixinTemplates = list(Wrappers.lambdaQuery(WeixinTemplate.class).in(WeixinTemplate::getTemplateId, templateIds));
        Assert.isTrue(weixinTemplates.size() == templateIds.size(), "模板数据有误,必须包含单人和双人模板");
        // 获取单人和双人模板，作为统一入参
        Map<String, List<WeixinTemplate>> maps = weixinTemplates.stream().collect(Collectors.groupingBy(WeixinTemplate::getTemplateType));
        JSONObject templateIdVos = JSONObject.of();
        maps.forEach((templateType, weixinTemplateVos) -> {
            List<Long> ids = weixinTemplateVos.stream().map(WeixinTemplate::getTemplateId).collect(Collectors.toList());
            templateIdVos.put(templateType, ids);
        });
        Assert.isTrue(templateIdVos.size() == 2, "缺少模板类型");
        // 构建回调参数，用于额外操作
        // 获取父节点id信息
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class).eq(WeixinRelatedContacts::getRelated1, wxIdA).or().eq(WeixinRelatedContacts::getRelated2, wxIdA));
        Assert.notNull(weixinRelatedContacts, "父节点信息异常");
        String wxId = weixinRelatedContacts.getWxId();
        WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
        WeixinAsyncEventCall old = weixinAsyncEventCallService.getOne(
                Wrappers.lambdaQuery(WeixinAsyncEventCall.class)
                        .eq(WeixinAsyncEventCall::getWxId, wxId)
                        .eq(WeixinAsyncEventCall::getEventType, ResConstant.ASYNC_EVENT_GROUP_CHAT)
                        // 获取正在处理的该微信数据
                        .eq(WeixinAsyncEventCall::getResultCode, "99"));
        Date delay = new Date();
        if (old != null) {
            if (old.getPlanTime().compareTo(LocalDateTime.now()) > 0) {
                // 直接提醒还存在待完成的数据，返回开始预计开始时间和预计完成时间
                log.info("该微信上次群聊还没执行完成：{}", wxId);
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
                if(fixedTime != null){
                    if (fixedTime.compareTo(new Date()) >  0) {
                        delay = fixedTime;
                    }
                }
                // 生成对应的批次号，重新赋值
                weixinAsyncEventCall = new WeixinAsyncEventCall()
                        // 群邀请类型
                        .setEventType(ResConstant.ASYNC_EVENT_GROUP_CHAT)
                        .setBusinessId(UUID.fastUUID().toString())
                        .setWxId(wxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            }
        } else {
            // 该微信首次创建话术，直接延时至指定时间
            if(fixedTime != null){
                if (fixedTime.compareTo(new Date()) >  0) {
                    delay = fixedTime;
                }
            }
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_GROUP_CHAT)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(wxId)
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
        for (String chatRoomName : chatRoomNames) {
            // 构建延时消息操作，暂时按照一个群5秒操作
            JSONObject jsonObject = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(),
                    "chatRoomName", chatRoomName,
                    "templateIds", templateIdVos);
            jsonObject.put("wxIdA", wxIdA);
            jsonObject.put("wxId", wxId);
            jsonObject.put("wxIdB", wxIdB);
            // 开始构建延时消息
            Message message = new Message(consumerTopic, groupChatTag, JSON.toJSONBytes(jsonObject));
            // 设置随机时间10-15秒执行时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, 32, 45);
            log.info("发送延时消息延时时间为：{}", delay);
            try {
                delayMqProducer.sendDelay(message, delay);
                // 记录已操作过的群聊信息，并标识为正在处理中
                WeixinTemplateSendDetail weixinTemplateSendDetail =
                        new WeixinTemplateSendDetail()
                                .setCreateTime(new Date())
                                .setWxId(wxId)
                                .setChatRoomId(chatRoomName)
                                .setStatus("99")
                                .setAsyncEventCallId(weixinAsyncEventCall.getAsyncEventCallId());
                // 先保存数据防止批量保存数据过多
                weixinTemplateSendDetailService.save(weixinTemplateSendDetail);
                // weixinTemplateSendDetails.add(weixinTemplateSendDetail);
            } catch (InterruptedException e) {
                log.error("mq发送延迟消息失败{}，wxId:{}", e.getMessage(), wxId);
                result.put("code", 500);
                result.put("msg", "mq发送延迟消息失败");
                // TODO 已发送出去消息还是正常处理，记录下发送成功的群id信息，防止二次发送
                return result;
            }
        }
        // 设置预期完成时间，用于后置添加进来的数据处理
        // 后边加入的微信进群操作需要
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
        result.put("planTime", weixinAsyncEventCall.getPlanTime());
        result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
        result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
        return result;
    }

    private void checkId(String wxIdA, String wxIdB) {
        WeixinBaseInfo weixinBaseInfoA = weixinBaseInfoService.getById(wxIdA);
        Assert.isTrue(!(weixinBaseInfoA == null || StrUtil.isEmpty(weixinBaseInfoA.getKey()) || !StrUtil.equals(weixinBaseInfoA.getState(), "1")), "子账号wxId" + wxIdA + "未登录系统或者不在线");
        WeixinBaseInfo weixinBaseInfoB = weixinBaseInfoService.getById(wxIdB);
        Assert.isTrue(!(weixinBaseInfoB == null || StrUtil.isEmpty(weixinBaseInfoB.getKey()) || !StrUtil.equals(weixinBaseInfoB.getState(), "1")), "子账号wxId" + wxIdB + "未登录系统或者不在线");
    }

    private void checkWxId(String wxId){
        // 先获取对应的子账号列表
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(wxId);
        String wxIdA = weixinRelatedContacts.getRelated1();
        String wxIdB = weixinRelatedContacts.getRelated2();
        // 获取对应的key值
        checkId(wxIdA, wxIdB);
    }

    @Transactional
    public boolean add(WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails) {
        if (save(weixinTemplate)) {
            // 填充id
            for (WeixinTemplateDetail weixinTemplateDetail : weixinTemplateDetails) {
                weixinTemplateDetail.setTemplateId(weixinTemplate.getTemplateId());
            }
        }
        // 批量新增
        return weixinTemplateDetailService.saveBatch(weixinTemplateDetails);
    }

    @Transactional
    public boolean update(WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails) {
        if (updateById(weixinTemplate)) {
            // 填充id,删除老数据
            weixinTemplateDetailService.removeByMap(JSONObject.of("template_id", weixinTemplate.getTemplateId()));
            for (WeixinTemplateDetail weixinTemplateDetail : weixinTemplateDetails) {
                weixinTemplateDetail.setTemplateId(weixinTemplate.getTemplateId());
            }
        }
        // 批量新增
        return weixinTemplateDetailService.saveBatch(weixinTemplateDetails);
    }

    public List<WeixinTemplateParam> queryList(WeixinTemplate weixinTemplate) {
        List<WeixinTemplateParam> weixinTemplateParams = Lists.newArrayList();
        List<WeixinTemplate> weixinTemplates = list(Wrappers.lambdaQuery(WeixinTemplate.class)
                .eq(StrUtil.isNotEmpty(weixinTemplate.getTemplateName()), WeixinTemplate::getTemplateName, weixinTemplate.getTemplateName())
                .eq(StrUtil.isNotEmpty(weixinTemplate.getTemplateType()), WeixinTemplate::getTemplateType, weixinTemplate.getTemplateType()));
        for (WeixinTemplate weixinTemplateVo : weixinTemplates) {
            WeixinTemplateParam weixinTemplateParam = new WeixinTemplateParam();
            weixinTemplateParam.setWeixinTemplate(weixinTemplateVo);
            if (weixinTemplateVo != null) {
                // 查询其模板详情
                weixinTemplateParam.setWeixinTemplateDetailList(weixinTemplateDetailService.listByMap(JSONObject.of("template_id", weixinTemplateVo.getTemplateId())));
            }
            weixinTemplateParams.add(weixinTemplateParam);
        }
        return weixinTemplateParams;
    }

    @Transactional
    public boolean deleteByName(String templateName) {
        // 查询数据
        WeixinTemplate weixinTemplate = getOne(Wrappers.lambdaQuery(WeixinTemplate.class).eq(WeixinTemplate::getTemplateName, templateName));
        if (weixinTemplate == null) return false;
        if (removeByMap(JSONObject.of("template_name", templateName))) {
            // 删除模板内容
            return weixinTemplateDetailService.removeByMap(JSONObject.of("template_id", weixinTemplate.getTemplateId()));
        }
        return false;
    }

}
