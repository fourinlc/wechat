package com.xxx.server.mq;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinGroupSendDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinGroupSendDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Objects;

@Component("groupSendTag")
@Slf4j
@AllArgsConstructor
public class GroupSendMqMessageHandler implements MqMessageHandler {

    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    private IWeixinGroupSendDetailService weixinGroupSendDetailService;

    @Override
    public boolean process(JSONObject message) {
        // 操作群链接对应的id
        MultiValueMap multiValueMap = new LinkedMultiValueMap();
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
            weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setStatus("500").setResult(weixinAsyncEventCall.getResult()));
            return true;
        }
        JSONObject chatRoomInfo = JSONObject.of("ChatRoomWxIdList", Lists.newArrayList(chatRoomId));
        multiValueMap.add("key", wxId);
        JSONObject chatRoomInfoRes = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(chatRoomInfo, multiValueMap);
        // 提取对应的群验证标识字段，并更新至邀请链接中
        //JSONObject contact = chatRoomInfoRes.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
        log.info("step two: 校验自己是否还在群中");
        if(!ResConstant.CODE_SUCCESS.equals(chatRoomInfoRes.getInteger(ResConstant.CODE))){
            // 结束后续操作，此处不在邀请子账号入群
            weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("主微信号不在群中").setStatus("500"));
            return true;
        }
        try {
            // 随机休眠1-2秒,
            Thread.sleep(RandomUtil.randomInt(500, 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //TODO 校验好友关系是否正常,自己账号是否无须验证
        if(slaveWxIds.size() > 0){
            // 邀请子号进群
            JSONObject jsonObject2 = JSONObject.of("ChatRoomName", chatRoomId, "UserList", slaveWxIds);
            JSONObject addChatroomMembers = WechatApiHelper.INVITE_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
            log.info("step three: 邀请子账号进群，返回值：{}", addChatroomMembers);
            if(!ResConstant.CODE_SUCCESS.equals(addChatroomMembers.getInteger(ResConstant.CODE))){
                // 结束后续操作，此处不在邀请子账号入群
                weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("邀请进群失败").setStatus("500"));
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("邀请进群失败"));
                return true;
            }
            if(flag){
                // 延迟几分钟自动进群
                log.info("自动进群开始==============================》》");
            }
        }
        // TODO 判断群是否是验证群，验证群跳过,或者是不是好友关系跳过
        // 更新群聊消息状态
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200));
        weixinGroupSendDetailService.updateById(weixinGroupSendDetail.setResult("处理成功").setStatus("200"));
        return true;
    }
}
