package com.xxx.server.mq;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import com.xxx.server.service.IWeixinRelatedContactsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 进群成功失败信息处理，tag为scanIntoUrlGroup处理类
 */
@Component("scanIntoUrlGroupTag")
@Slf4j
@AllArgsConstructor
public class ScanIntoUrlGroupMessageHandler implements MqMessageHandler {

    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    private IWeixinRelatedContactsService weixinRelatedContactsService;

    @Override
    public boolean process(JSONObject message) {
        // log.info("开始进群消息处理====》{}", message);
        String code = message.getString("code");
        // 操作群链接对应的id
        Long linkId = message.getLong("linkId");
        WechatApiHelper wechatApiHelper = WechatApiHelper.getWechatApiHelper(code);
        if (wechatApiHelper == null) {
            log.info("群消息体数据异常,跳过处理：{}", message);
            return true;
        }
        LinkedTreeMap query = (LinkedTreeMap) message.get("query");
        JSONObject param = message.getJSONObject("param");
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap(query);
        // TODO 增加校验该用户是否还在群中，从获取联系人列表查看自己是否在这个群里
        // Date delay = new Date();
        // 校验该批次是否还是有些状态
        Long asyncEventCallId = message.getLong("asyncEventCallId");
        WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
        if (Objects.isNull(weixinAsyncEventCall)) {
            log.info("数据格式不正确,忽略该数据");
            return true;
        }
        if (weixinAsyncEventCall.getResultCode() == 500) {
            log.info("流程提前结束：{}", message);
            return true;
        }
        // 额外校验群链接生效情况
        JSONObject data = wechatApiHelper.invoke(param, multiValueMap);
        log.info("step one: 主号自己进群,返回值：{}", data);
        if (ResConstant.CODE_SUCCESS.equals(data.getInteger(ResConstant.CODE)) && "进群成功".equals(data.getString("Text"))) {
            // 更新链接状态为进群完成
            weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("1").setLinkId(linkId));
            JSONObject jsonObject = data.getJSONObject(ResConstant.DATA);
            // Assert.isTrue(Objects.nonNull(jsonObject), "群操作数据结构异常");
            String chatroomUrl = jsonObject.getString("chatroomUrl");
            log.info("chatroomUrl:{}", chatroomUrl);
            Matcher matcher = Pattern.compile(ResConstant.PATTERN).matcher(chatroomUrl);
            if (matcher.find()) {
                String chatroomName = matcher.group();
                log.info("chatroomName:{}", chatroomName);
                // 查看自己是否还在群中，同时也相当于获取群聊信息，确定群是验证群还是普通群，间隔时间设置成0.5-1秒之间
                JSONObject chatRoomInfo = JSONObject.of("ChatRoomWxIdList", Lists.newArrayList(chatroomName));
                try {
                    // 随机休眠0.5-1秒
                    Thread.sleep(RandomUtil.randomInt(500, 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JSONObject chatRoomInfoRes = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(chatRoomInfo, multiValueMap);
                // 提取对应的群验证标识字段，并更新至邀请链接中
                JSONObject contact = chatRoomInfoRes.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
                log.info("step two: 校验是否还在群中同时更新群是否验证标识:群信息{}", contact);
                String chatroomAccessType = contact.getString("chatroomAccessType");
                // 更新群验证状态
                weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setVerifyStatus(chatroomAccessType).setLinkId(linkId));
                // 开始组装保存群聊
                // 进群成功后，保存群聊，更新邀请链接状态
                // 延迟2-3秒，开始保存群聊信息，组装对应参数，获取群id
                JSONObject jsonObject1 = JSONObject.of("ChatRoomName", chatroomName, "Val", 1);
                // 随机延时1-2操作
                // RandomUtil.randomDate(new Date(), DateField.SECOND, 1, 2);
                //TODO 直接调用,异步解耦调用
                try {
                    // 随机休眠1-2秒
                    Thread.sleep(RandomUtil.randomInt(500, 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JSONObject movetoContract = WechatApiHelper.MOVETO_CONTRACT.invoke(jsonObject1, multiValueMap);
                log.info("step three: 保存群聊信息返回{}", movetoContract);
                if (ResConstant.CODE_SUCCESS.equals(movetoContract.getInteger(ResConstant.CODE))) {
                    // 更新系统状态批次状态
                    // 更新链接状态为保存群聊消息
                    weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("2").setLinkId(linkId));
                    // 保存群聊成功时，邀请两个子号进行进群操作
                    try {
                        Thread.sleep(RandomUtil.randomInt(1000, 1500));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 获取子账号信息，如果不存在，则无须邀请进群了
                    WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(weixinAsyncEventCall.getWxId());
                    if (!(StrUtil.isEmpty(weixinRelatedContacts.getRelated1()) || StrUtil.isEmpty(weixinRelatedContacts.getRelated2()))) {
                        JSONObject jsonObject2 = JSONObject.of("ChatRoomName", chatroomName, "UserList", Lists.newArrayList(weixinRelatedContacts.getRelated1(), weixinRelatedContacts.getRelated2()));
                        // TODO 判断群是否是验证群，验证群跳过,或者是不是好友关系跳过
                        JSONObject addChatroomMembers = WechatApiHelper.INVITE_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
                        log.info("step three: 邀请子账号进群，返回值：{}", addChatroomMembers);
                    }
                    // WechatApiHelper.ADD_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
                    // 更新群聊消息状态
                    weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("4").setLinkId(linkId));
                    weixinAsyncEventCall.setResultCode(200);
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
                    return true;
                }
            }
        }
        // 根据code值更新对应的邀请链接状态
        // TODO 带数据验证
        weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("5").setLinkId(linkId));
        // 更新系统批次为失败状态，下次该批次直接跳过
        weixinAsyncEventCall.setResultCode(500);
        weixinAsyncEventCall.setRealTime(LocalDateTime.now()).setResult("进群操作失败");
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
        return true;
    }

}
