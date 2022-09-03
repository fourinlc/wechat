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
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinDictionaryService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
public class WeixinGroupLinkDetailServiceImpl extends ServiceImpl<WeixinGroupLinkDetailMapper, WeixinGroupLinkDetail> implements IWeixinGroupLinkDetailService {

    @Resource
    private IWeixinDictionaryService weixinDictionaryService;

    @Resource
    private DelayMqProducer delayMqProducer;

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.qunGroupNew}")
    private String consumerQunGroupTag;

    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;

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
                        .setWxId(wxId)
                        .setPlanStartTime(LocalDateTimeUtil.of(delay))
                        // 设置99为处理中状态
                        .setResultCode(99);
                weixinAsyncEventCallService.save(weixinAsyncEventCall);
            } else if (planTime.compareTo(LocalDateTime.now()) > 0) {
                // 直接提醒还存在待完成的数据，返回开始预计开始时间和预计完成时间
                log.info("该微信上次群聊还没执行完成：{}", wxId);
                result.put("code", 500);
                result.put("planTime", weixinAsyncEventCall.getPlanTime());
                result.put("planStartTime", weixinAsyncEventCall.getPlanStartTime());
                result.put("msg", "该微信上次批量拉群还没执行完成");
                result.put("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId());
                return result;
            } else {
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
        if (weixinAsyncEventCall.getPlanTime() != null) {
            // 重置老数据直接添加至队尾
            delay = DateUtil.date(weixinAsyncEventCall.getPlanTime());
        }
        // 获取群链接列表
        List<WeixinGroupLinkDetail> weixinGroupLinkDetailList = listByIds(linkIds);
        for (int i = 0; i < weixinGroupLinkDetailList.size(); i++) {
            if ((i + 1) % (sheaves * rate) == 0) {
                // log.info("新的一轮操作：{}", i);
                // 说明一轮数据完成，增加间隔时间
                delay = DateUtils.addSeconds(delay, between * 60);
            }
            WeixinGroupLinkDetail weixinGroupLinkDetail = weixinGroupLinkDetailList.get(i);
            // 获取对应的key值信息
            JSONObject msg = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(),
                    "linkId", weixinGroupLinkDetail.getLinkId(),
                    "wxIds", wxIds);
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
            } catch (Exception e) {
                log.error("消息处理失败:{}", e.getMessage());
                // 重置回调参数异步回调参数，更新为处理失败
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("mq发送消息失败"));
                baseMapper.updateById(weixinGroupLinkDetail.setLinkStatus("5"));
                throw new BusinessException("mq消息发送失败，请检测网络情况");
            }
        }
        // 后边加入的微信进群操作需要
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setPlanTime(LocalDateTimeUtil.of(delay)));
        result.put("planTime", weixinAsyncEventCall.getPlanTime());
        return result;
    }

    @Override
    public boolean saveBatch(List<WeixinGroupLinkDetail> weixinGroupLinkDetails) {
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
        List<WeixinGroupLinkDetail> weixinGroupLinkDetails = list(Wrappers.<WeixinGroupLinkDetail>lambdaQuery()
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvitationTime()), WeixinGroupLinkDetail::getInvitationTime, weixinGroupLinkDetail.getInvitationTime())
                .like(StrUtil.isNotEmpty(weixinGroupLinkDetail.getFromUserName()), WeixinGroupLinkDetail::getFromUserName, weixinGroupLinkDetail.getFromUserName())
                .in(StrUtil.isNotEmpty(weixinGroupLinkDetail.getLinkStatus()), WeixinGroupLinkDetail::getLinkStatus, linkStatus)
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getToUserWxId()), WeixinGroupLinkDetail::getToUserWxId, weixinGroupLinkDetail.getToUserWxId())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvalidStatus()), WeixinGroupLinkDetail::getInvalidStatus, weixinGroupLinkDetail.getInvalidStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getVerifyStatus()), WeixinGroupLinkDetail::getVerifyStatus, weixinGroupLinkDetail.getVerifyStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getRepeatStatus()), WeixinGroupLinkDetail::getRepeatStatus, weixinGroupLinkDetail.getRepeatStatus())
                .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getCompanyStatus()), WeixinGroupLinkDetail::getCompanyStatus, weixinGroupLinkDetail.getCompanyStatus()));
        // 解析每个群的子账户进群情况,
        // 先校验子账号绑定情况，如果都没绑定跳过这个步骤
       /* String wxId = weixinGroupLinkDetail.getToUserWxId();
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(wxId);
        if(weixinRelatedContacts == null) return weixinGroupLinkDetails;
        String related2 = weixinRelatedContacts.getRelated2();
        String related1 = weixinRelatedContacts.getRelated1();
        List<String> wxIds = Lists.newArrayList(wxId);
        if(StrUtil.isNotEmpty(related1)){
            wxIds.add(related1);
        }
        if(StrUtil.isNotEmpty(related2)){
            wxIds.add(related2);
        }
        if (wxIds.size() > 1) {
            // 存在子号情况，过滤操作成功的群，查看具体进群情况
            List<String> weixinGroupLinkDetailList = weixinGroupLinkDetails.stream()
                    //.filter(weixinGroupLinkDetail1 -> StrUtil.equals("1", weixinGroupLinkDetail1.getLinkStatus()))
                    .map(WeixinGroupLinkDetail::getChatroomName)
                    .distinct()
                    .collect(Collectors.toList());
            if(weixinGroupLinkDetailList.size() == 0) return weixinGroupLinkDetails;
            // 组装查询微信群列表信息
            JSONObject chatRoomInfo = JSONObject.of("ChatRoomWxIdList", Lists.newArrayList(weixinGroupLinkDetailList));
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap();
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(wxId);
            JSONObject userNamesVo = JSONObject.of();
            // 校验其在线状态
            if(weixinBaseInfo != null && StrUtil.equals("1", weixinBaseInfo.getState())){
                multiValueMap.add("key", weixinBaseInfo.getKey());
                JSONObject jsonObject = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(chatRoomInfo, multiValueMap);
                if(ResConstant.CODE_SUCCESS.equals(jsonObject.getInteger(ResConstant.CODE))){
                    JSONArray contactList = jsonObject.getJSONObject(ResConstant.DATA).getJSONArray("contactList");
                    for (Object o1 : contactList) {
                        Map map = (Map)o1;
                        JSONObject jsonObject1 = new JSONObject(map);
                        String chatRoomId = jsonObject1.getString("userName");
                        // 真正的群成员列表
                        JSONArray jsonArray = jsonObject1.getJSONObject("newChatroomData").getJSONArray("chatroom_member_list");
                        // 校验这个群的子账号是否都在其中
                        List<String> userNames = jsonArray.stream().filter(o -> {
                            Map map2 = (Map)o;
                            JSONObject data = new JSONObject(map2);
                            return wxIds.contains(data.getString("user_name"));
                        }).map(o -> {
                            Map map2 = (Map)o;
                            JSONObject data = new JSONObject(map2);
                            return data.getString("user_name");
                        }).collect(Collectors.toList());
                        // 顺便构造对应的基本参数列表
                        List<WeixinBaseInfo> weixinBaseInfoList = weixinBaseInfoService.listByIds(userNames);
                        userNamesVo.put(chatRoomId, weixinBaseInfoList);
                    }
                }
            }

            JSONArray jsonArray = new JSONArray();
            for (WeixinGroupLinkDetail groupLinkDetail : weixinGroupLinkDetails) {
                String chatroomName = groupLinkDetail.getChatroomName();
                JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(groupLinkDetail));
                // 从中获取存在的好友信息 userNamesVo
                jsonObject.put("userNames", userNamesVo.get(chatroomName));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }*/
        return weixinGroupLinkDetails;
    }

}