package com.xxx.server.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.mapper.WeixinTemplateSendDetailMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-08-16
 */
@Service
@Slf4j
public class WeixinTemplateSendDetailServiceImpl extends ServiceImpl<WeixinTemplateSendDetailMapper, WeixinTemplateSendDetail> implements IWeixinTemplateSendDetailService {

    @Resource
    private IWeixinRelatedContactsService weixinRelatedContactsService;
    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;
    @Resource
    private IWeixinTemplateService weixinTemplateService;
    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;
    @Resource
    @Lazy
    private IWeixinTemplateSendDetailService weixinTemplateSendDetailService;

    // 获取群发或者待群发列表
    public List<WeixinTemplateSendDetail> queryList(String wxId, boolean refresh) {
        List<WeixinBaseInfo> weixinBaseInfos = weixinRelatedContactsService.queryRelatedList(wxId);
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class).eq(WeixinRelatedContacts::getRelated1, wxId).or().eq(WeixinRelatedContacts::getRelated2, wxId));
        List<WeixinContactDetailedInfo> weixinContactDetailedInfos = Lists.newArrayList();
        List<WeixinTemplateSendDetail> weixinTemplateSendDetails = Lists.newArrayList();
        if (weixinBaseInfos.size() == 1) {
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfos.get(0);
            // 暂时未关联或者已关联但只有一个账号
            RespBean friendsAndChatRooms = weixinBaseInfoService.getFriendsAndChatRooms(weixinBaseInfo.getKey(), weixinBaseInfo.getWxId(), refresh);
            JSONObject jsonObject = (JSONObject) friendsAndChatRooms.getObj();
            JSONArray chatRoomDetaile = jsonObject.getJSONArray("chatRoomDetaile");
            weixinContactDetailedInfos.addAll(chatRoomDetaile.toList(WeixinContactDetailedInfo.class));
            weixinContactDetailedInfos.forEach(weixinContactDetailedInfo -> {
                // 构建具体的数据
                WeixinTemplateSendDetail weixinTemplateSendDetail = new WeixinTemplateSendDetail()
                        .setChatRoomId(weixinContactDetailedInfo.getWxId())
                        .setChatRoomName(weixinContactDetailedInfo.getUserName())
                        .setHeadImgUrl(weixinContactDetailedInfo.getSmallHeadImgUrl());
                weixinTemplateSendDetails.add(weixinTemplateSendDetail);
            });
            return weixinTemplateSendDetails;
        } else {
            // 存在关联信息且已关联,先去存储数据表中获取是否已经生成模板信息
            List<WeixinTemplateSendDetail> weixinTemplateSendDetailsVo = getBaseMapper().selectList(
                    Wrappers.lambdaQuery(WeixinTemplateSendDetail.class)
                            .eq(WeixinTemplateSendDetail::getWxId, weixinRelatedContacts.getWxId())
                            .eq(WeixinTemplateSendDetail::getCreateTime, DateUtil.today()));
            // 构造模板参数并入库
            // 查询两个微信的所有群信息详情
            for (WeixinBaseInfo weixinBaseInfo : weixinBaseInfos) {
                // 校验账号登录情况
                if (!StrUtil.equals(weixinBaseInfo.getState(), "1")) continue;
                RespBean friendsAndChatRooms = weixinBaseInfoService.getFriendsAndChatRooms(weixinBaseInfo.getKey(), weixinBaseInfo.getWxId(), refresh);
                JSONObject jsonObject = (JSONObject) friendsAndChatRooms.getObj();
                JSONArray chatRoomDetaile = jsonObject.getJSONArray("chatRoomDetaile");
                weixinContactDetailedInfos.addAll(chatRoomDetaile.toList(WeixinContactDetailedInfo.class));
            }
            // 去重相同的数据
            weixinContactDetailedInfos.stream().distinct().forEach(weixinContactDetailedInfo -> {
                // 构建具体的数据
                WeixinTemplateSendDetail weixinTemplateSendDetail = new WeixinTemplateSendDetail()
                        .setChatRoomId(weixinContactDetailedInfo.getWxId())
                        .setChatRoomName(weixinContactDetailedInfo.getUserName())
                        .setHeadImgUrl(weixinContactDetailedInfo.getSmallHeadImgUrl());
                weixinTemplateSendDetails.add(weixinTemplateSendDetail);
                // 如果包含在列表中，更新对应的模板
                weixinTemplateSendDetailsVo
                        .stream()
                        .filter(weixinTemplateSendDetailVo -> StrUtil.equals(weixinTemplateSendDetailVo.getChatRoomId(), weixinContactDetailedInfo.getWxId()))
                        .findAny()
                        .ifPresent(templateSendDetail -> {
                            weixinTemplateSendDetail.setTemplateId(templateSendDetail.getTemplateId());
                            // 查询对应的模板名称
                            WeixinTemplate weixinTemplate = weixinTemplateService.getById(templateSendDetail.getTemplateId());
                            // 存在正在处理中情况
                            if (weixinTemplate != null) {
                                weixinTemplateSendDetail.setTemplateName(weixinTemplate.getTemplateName());
                            }
                            weixinTemplateSendDetail.setStatus(templateSendDetail.getStatus());
                            weixinTemplateSendDetail.setFinishTime(templateSendDetail.getFinishTime());
                            weixinTemplateSendDetail.setResult(templateSendDetail.getResult());
                        });
                // 排序操作
                weixinTemplateSendDetails.sort(Comparator.comparing(WeixinTemplateSendDetail::getStatus, Comparator.nullsFirst(String::compareTo)).reversed());
            });
            return weixinTemplateSendDetails;
        }
    }

    public List<WeixinTemplateSendDetail> queryList(Long asyncEventCallId) {
        // 查询当前微信是否存在拉群操作
        WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
        Assert.notNull(weixinAsyncEventCall, "该批次号不存在");
        // 获取该批次所有的列表
        List<WeixinTemplateSendDetail> weixinTemplateSendDetails = weixinTemplateSendDetailService.list(Wrappers.lambdaQuery(WeixinTemplateSendDetail.class).eq(WeixinTemplateSendDetail::getAsyncEventCallId, weixinAsyncEventCall.getAsyncEventCallId()));
        for (WeixinTemplateSendDetail weixinTemplateSendDetail : weixinTemplateSendDetails) {
            // 填充模板名
            WeixinTemplate weixinTemplate = weixinTemplateService.getById(weixinTemplateSendDetail.getTemplateId());
            if (weixinTemplate != null) {
                weixinTemplateSendDetail.setTemplateName(weixinTemplate.getTemplateName());
            }
        }
        return weixinTemplateSendDetails;
    }
}
