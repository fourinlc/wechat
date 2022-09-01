package com.xxx.server.mq;

import cn.hutool.core.date.DateField;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dachen.starter.mq.custom.consumer.DelayMqConsumer;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.pojo.WeixinGroupSendDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import com.xxx.server.service.IWeixinGroupSendDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 批量拉群操作
 */
@Component("groupSendTag")
@Slf4j
public class GroupSendMqMessageHandler implements MqMessageHandler {

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Resource
    private IWeixinGroupSendDetailService weixinGroupSendDetailService;

    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;

    @Resource
    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @Resource
    private DelayMqProducer delayMqProducer;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.rocketmq.tags.qunGroupNew}")
    private String qunGroupNew;

    @Override
    public boolean process(JSONObject message) {
        try {
            // 操作群链接对应的id
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
            String wxId = message.getString("wxId");
            List<String> slaveWxIds = message.getList("slaveWxIds", String.class);
            String chatRoomId = message.getString("chatRoomId");
            Boolean flag = message.getBoolean("flag");
            // Date delay = new Date();
            // 校验该批次是否还是有些状态
            Long asyncEventCallId = message.getLong("asyncEventCallId");
            Long groupSendDetailId = message.getLong("groupSendDetailId");
            WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
            WeixinGroupSendDetail weixinGroupSendDetail = weixinGroupSendDetailService.getById(groupSendDetailId);
            if (Objects.isNull(weixinAsyncEventCall) || Objects.isNull(weixinGroupSendDetail) || weixinAsyncEventCall.getResultCode() == 500) {
                log.info("流程提前结束：{}", message);
                // 更新原始数据进群详情信息,增加描述信息
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("流程提前结束"));
                weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setStatus("500").setResult(weixinAsyncEventCall.getResult()));
                return true;
            }
            JSONObject chatRoomInfo = JSONObject.of("ChatRoomWxIdList", Lists.newArrayList(chatRoomId));
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(wxId);
            if (weixinBaseInfo == null || !StrUtil.equals(weixinBaseInfo.getState(), "1")) {
                // 结束整个流程
                log.info("主号已掉线");
                weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("主号已掉线").setStatus("500"));
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("主号已掉线"));
                return true;
            }
            multiValueMap.add("key", weixinBaseInfo.getKey());
            JSONObject chatRoomInfoRes = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(chatRoomInfo, multiValueMap);
            // 提取对应的群验证标识字段，并更新至邀请链接中
            //JSONObject contact = chatRoomInfoRes.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
            log.info("step two: 校验自己是否还在群中");
            String nickName = "";
            if (!ResConstant.CODE_SUCCESS.equals(chatRoomInfoRes.getInteger(ResConstant.CODE))) {
                log.info("主微信号不在群中");
                // 结束后续操作，此处不在邀请子账号入群
                weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("主微信号不在群中").setStatus("500"));
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("主微信号不在群中"));
                return true;
            } else {
                // 获取下群名称
                JSONObject contact = chatRoomInfoRes.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
                nickName = contact.getJSONObject("nickName").getString("str");
                if (StrUtil.isEmpty(nickName)) {
                    log.info("主微信号不在群中");
                    weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("主微信号不在群中").setStatus("500"));
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("主微信号不在群中"));
                    return true;
                }
            }
            try {
                // 随机休眠1-2秒,
                Thread.sleep(RandomUtil.randomInt(500, 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //TODO 校验好友关系是否正常,自己账号是否无须验证
            if (slaveWxIds.size() > 0) {
                // 邀请子号进群
                JSONObject jsonObject2 = JSONObject.of("ChatRoomName", chatRoomId, "UserList", slaveWxIds);
                JSONObject addChatroomMembers = WechatApiHelper.INVITE_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
                log.info("step three: 邀请子账号进群，返回值：{}", addChatroomMembers);
                if (!ResConstant.CODE_SUCCESS.equals(addChatroomMembers.getInteger(ResConstant.CODE))) {
                    // 结束后续操作，此处不在邀请子账号入群
                    log.info("邀请子号进群失败");
                    weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("邀请进群失败").setStatus("500"));
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("邀请进群失败"));
                    return true;
                }
                if (flag) {
                    log.info("自动进群开始==============================》》");
                    // 延迟几分钟自动进群,获取对应的配置项
                    // 根据发送者、接收者以及群名，获取一个时刻唯一一条邀请链接，并将状态置为自动处理
                    for (String slaveWxId : slaveWxIds) {
                        byte[] nickNameDecode = Base64.getEncoder().encode(nickName.getBytes(StandardCharsets.UTF_8));
                        JSONObject paramVo = JSONObject.of("toUserWxId", slaveWxId, "fromUserWxId", wxId, "chatroomName", new String(nickNameDecode));
                        // 分别进群并保存群聊至通讯录
                        // 模拟延时90秒到120秒进群
                        Date delay = RandomUtil.randomDate(new Date(), DateField.SECOND, 60, 90);
                        JSONObject msg = JSONObject.of("asyncEventCallId", weixinAsyncEventCall.getAsyncEventCallId(), "paramVo", paramVo);
                        // 用于回调子账号进群情况
                        msg.put("groupSendDetailId", groupSendDetailId);
                        Message param = new Message(consumerTopic, qunGroupNew, JSON.toJSONBytes(msg));
                        // 从邀请列表中获取对应的群链接
                        log.info("开始自动进群消息：{}", param);
                        try {
                            delayMqProducer.sendDelay(param, delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // 更新群聊消息状态
            if (weixinAsyncEventCall.getPlanTime() != null && LocalDateTime.now().compareTo(weixinAsyncEventCall.getPlanTime()) >= 0) {
                log.info("该轮群发完成");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200).setResult("群发完成").setRealTime(LocalDateTime.now()));
                weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("处理成功").setStatus("200"));
                return true;
            }
            log.info("该群处理完成，群名：{}", nickName);
            weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("处理成功").setStatus("200"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
