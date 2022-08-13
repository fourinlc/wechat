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
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinTemplateMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.Collection;
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

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.groupChat}")
    private String groupChatTag;

    @Override
    public boolean groupChat(List<String> chatRoomNames, String wxId, List<Long> templateIds) {
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
        if (old != null) {
            weixinAsyncEventCall = old;
            // 获取计划完成时间参数
        } else {
            // 生成对应的批次号
            weixinAsyncEventCall
                    // 群邀请类型
                    .setEventType(ResConstant.ASYNC_EVENT_GROUP_CHAT)
                    .setBusinessId(UUID.fastUUID().toString())
                    .setWxId(wxId)
                    // 设置99为处理中状态
                    .setResultCode(99);
            weixinAsyncEventCallService.saveOrUpdate(weixinAsyncEventCall);
        }
        // 后来数据添加至前边操作
        Date delay = new Date();
        if (weixinAsyncEventCall.getPlanTime() != null) {
            // 重置老数据直接添加至队尾
            delay = DateUtil.date(weixinAsyncEventCall.getPlanTime());
        }
        for (String chatRoomName : chatRoomNames) {
            // 构建延时消息操作，暂时按照一个群5秒操作
            JSONObject jsonObject = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(),
                    "chatRoomName", chatRoomName,
                    "templateIds", templateIds);
            // 开始构建延时消息
            Message message = new Message(consumerTopic, groupChatTag, JSON.toJSONBytes(jsonObject));
            // 设置随机时间5-7秒执行时间
            delay = RandomUtil.randomDate(delay, DateField.SECOND, 5, 7);
            log.info("发送延时消息延时时间为：{}", delay);
            try {
                delayMqProducer.sendDelay(message, delay);
            } catch (InterruptedException e) {
                log.error("发送消息失败{}", e.getMessage());
                return false;
            }
        }
        // 设置预期完成时间，用于后置添加进来的数据处理
        // 后边加入的微信进群操作需要
        return weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
    }

     @Transactional
    public boolean add(WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails){
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
    public boolean update( WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails){
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

    public List<WeixinTemplateParam> queryList(WeixinTemplate weixinTemplate){
        List<WeixinTemplateParam> weixinTemplateParams = Lists.newArrayList();
        List<WeixinTemplate> weixinTemplates = list(Wrappers.lambdaQuery(WeixinTemplate.class)
                .eq(StrUtil.isNotEmpty(weixinTemplate.getTemplateName()), WeixinTemplate::getTemplateName, weixinTemplate.getTemplateName())
                .eq(StrUtil.isNotEmpty(weixinTemplate.getTemplateType()), WeixinTemplate::getTemplateType, weixinTemplate.getTemplateType()));
        for (WeixinTemplate weixinTemplateVo : weixinTemplates) {
            WeixinTemplateParam weixinTemplateParam = new WeixinTemplateParam();
            weixinTemplateParam.setWeixinTemplate(weixinTemplateVo);
            if(weixinTemplateVo != null){
                // 查询其模板详情
                weixinTemplateParam.setWeixinTemplateDetailList(weixinTemplateDetailService.listByMap(JSONObject.of("template_id", weixinTemplateVo.getTemplateId())));
            }
            weixinTemplateParams.add(weixinTemplateParam);
        }
        return weixinTemplateParams;
    }

    @Transactional
    public boolean deleteByName(String templateName){
         // 查询数据
        WeixinTemplate weixinTemplate = getOne(Wrappers.lambdaQuery(WeixinTemplate.class).eq(WeixinTemplate::getTemplateName, templateName));
        if(weixinTemplate == null) return false;
        if (removeByMap(JSONObject.of("template_name", templateName))) {
            // 删除模板内容
            return weixinTemplateDetailService.removeByMap(JSONObject.of("template_id", weixinTemplate.getTemplateId()));
        }
        return false;
    }

}
