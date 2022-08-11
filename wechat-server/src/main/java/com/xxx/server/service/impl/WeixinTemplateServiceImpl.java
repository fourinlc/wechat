package com.xxx.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinTemplateMapper;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.pojo.WeixinTemplateDetail;
import com.xxx.server.pojo.WeixinTemplateParam;
import com.xxx.server.service.IWeixinFileService;
import com.xxx.server.service.IWeixinRelatedContactsService;
import com.xxx.server.service.IWeixinTemplateDetailService;
import com.xxx.server.service.IWeixinTemplateService;
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

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    // @Override
    public void chatHandler(List<String> chatRoomNames, String wxId, List<Long> templateIds) throws InterruptedException {
        // 查询其对应的子号信息
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(wxId);
        Assert.isTrue(StrUtil.isNotEmpty(weixinRelatedContacts.getRelated1()) && StrUtil.isNotEmpty(weixinRelatedContacts.getRelated2()), "请先关联好友再操作");
        // 获取对应文件信息
        List<WeixinTemplateDetail> list = weixinTemplateDetailService.list(Wrappers.lambdaQuery(WeixinTemplateDetail.class).in(WeixinTemplateDetail::getTemplateId, templateIds));
        // 对具体模板进行分组
        Map<Long, List<WeixinTemplateDetail>> WeixinTemplateDetailMap = list.stream().collect(Collectors.groupingBy(WeixinTemplateDetail::getTemplateId));
        Collection<List<WeixinTemplateDetail>> values = WeixinTemplateDetailMap.values();
        List<List<WeixinTemplateDetail>> lists = Lists.newArrayList(values);
        for (int i = 0; i < chatRoomNames.size(); i++) {
            String chatRoomName = chatRoomNames.get(i);
            // 查询两个号码是否在群内
            // step one 遍历模板列表
            List<WeixinTemplateDetail> weixinTemplateDetails = lists.get(i % lists.size());
            // 发送延时消息至rocketmq
            JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            Date delay = new Date();
            // 每隔两秒执行一次
            String code = WechatApiHelper.SEND_TEXT_MESSAGE.getCode();
            for (WeixinTemplateDetail weixinTemplateDetail : weixinTemplateDetails) {
                // 构造模板参数
                // query.add("key", "A".equals(weixinTemplateDetail.getMsgRole()) ? keyA : keyB);
                // 1默认为普通文字消息
                if ("1".equals(weixinTemplateDetail.getMsgType())) {
                    param.put("AtWxIDList", null);
                    param.put("MsgType", 1);
                    param.put("TextContent", weixinTemplateDetail.getMsgContent());
                    // 发送文字信息
                    // WechatApiHelper.SEND_TEXT_MESSAGE.invoke(param, query);
                } else {
                    param.put("TextContent", "");
                    // 获取图片信息用于展示
                    param.put("ImageContent", weixinTemplateDetail.getMsgContent());
                    code = WechatApiHelper.SEND_IMAGE_MESSAGE.getCode();
                }
                // step two 校验AB账号登录状态,发送消息的时候是否会自动校验
                JSONObject msg = JSONObject.of("param", param, "query", query, "code", code);
                //TODO 是否有必要设置成异步消息,加快响应时间
                Message message = new Message(consumerTopic, "", JSON.toJSONBytes(msg));
                // 冲配置字典获取
                delay = DateUtils.addSeconds(delay, 2);
                delayMqProducer.sendDelay(message, delay);
                // 清空param、query参数
                param.clear();
                query.clear();
            }
        }
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
