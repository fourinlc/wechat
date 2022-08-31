package com.xxx.server.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 进群成功失败信息处理，tag为scanIntoUrlGroup处理类
 */
@Component("scanIntoUrlGroupNewTag")
@Slf4j
@AllArgsConstructor
public class ScanIntoUrlGroupNewMessageHandler implements MqMessageHandler {

    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    private IWeixinBaseInfoService weixinBaseInfoService;

    @Override
    public boolean process(JSONObject message) {
        try {
            MultiValueMap multiValueMap = new LinkedMultiValueMap();
            // 操作群链接对应的id
            Long linkId = message.getLong("linkId");
            // Date delay = new Date();
            // 校验该批次是否还是有些状态
            Long asyncEventCallId = message.getLong("asyncEventCallId");
            // 子号信息
            List<String> wxIds = message.getList("wxIds", String.class);
            WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
            WeixinGroupLinkDetail weixinGroupLinkDetail = weixinGroupLinkDetailService.getById(linkId);
            if (Objects.isNull(weixinAsyncEventCall) || Objects.isNull(weixinGroupLinkDetail) || weixinAsyncEventCall.getResultCode() == 500) {
                log.info("流程提前结束：{}", message);
                // 更新原始数据进群详情信息,增加描述信息
                weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setLinkStatus("500").setResult(weixinAsyncEventCall.getResult()));
                return true;
            }
            String toUserWxId = weixinGroupLinkDetail.getToUserWxId();
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(toUserWxId);
            // 获取对应的key值
            if (weixinBaseInfo == null || !StrUtil.equals(weixinBaseInfo.getState(), "1")) {
                // 结束整个流程
                log.info("主号已掉线");
                weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult("主号已掉线").setLinkStatus("500"));
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("主号已掉线"));
                return true;
            }
            multiValueMap.add("key", weixinBaseInfo.getKey());
            // 额外校验群链接生效情况
            JSONObject data = WechatApiHelper.SCAN_INTO_URL_GROUP.invoke(JSONObject.of("Url", weixinGroupLinkDetail.getContent()), multiValueMap);
            log.info("step one: 主号自己进群,返回值：{}", data);
            if (!(ResConstant.CODE_SUCCESS.equals(data.getInteger(ResConstant.CODE)) && "进群成功".equals(data.getString("Text")))) {
                log.info("主号自己进群失败");
                weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult("主号自己进群失败").setLinkStatus("500"));
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("主号自己进群失败"));
                return true;
            }
            // 更新链接状态为进群完成
            weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("1").setLinkId(linkId));
            JSONObject jsonObject = data.getJSONObject(ResConstant.DATA);
            // Assert.isTrue(Objects.nonNull(jsonObject), "群操作数据结构异常");
            String chatroomUrl = jsonObject.getString("chatroomUrl");
            log.info("提取群链接：chatroomUrl:{}", chatroomUrl);
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
                if (!ResConstant.CODE_SUCCESS.equals(chatRoomInfoRes.getInteger(ResConstant.CODE))) {
                    // 自己不在群中了，结束群聊操做
                    log.info("已经不在群中了");
                    weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult("已经不在群中").setLinkStatus("500"));
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("已经不在群中"));
                    return true;
                }
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
                if (!ResConstant.CODE_SUCCESS.equals(movetoContract.getInteger(ResConstant.CODE))) {
                    // 自己不在群中了，结束群聊操做
                    log.info("保存群聊失败");
                    weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult("保存群聊失败").setLinkStatus("500"));
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("保存群聊失败"));
                    return true;
                }
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
                if (!(wxIds == null || wxIds.isEmpty())) {
                    log.info("开始子号进群");
                    JSONObject jsonObject2 = JSONObject.of("ChatRoomName", chatroomName, "UserList", wxIds);
                    // TODO 判断群是否是验证群，验证群跳过,或者是不是好友关系跳过
                    JSONObject addChatroomMembers = WechatApiHelper.INVITE_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
                    if (!ResConstant.CODE_SUCCESS.equals(addChatroomMembers.getInteger(ResConstant.CODE))) {
                        // 自己不在群中了，结束群聊操做
                        log.info("邀请子号进群异常");
                        weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult("邀请子号进群异常").setLinkStatus("500"));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("邀请子号进群异常"));
                    }
                }else {
                    log.info("没有子号进群，校验是否是完成");
                    // 更新群聊消息状态
                    weixinGroupLinkDetailService.updateById(new WeixinGroupLinkDetail().setLinkStatus("4").setLinkId(linkId));
                    weixinAsyncEventCall.setResultCode(200);
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
                    log.info("now:{},PlanTime:{}", LocalDateTime.now(), weixinAsyncEventCall);
                }
                /*if (LocalDateTime.now().compareTo(weixinAsyncEventCall.getPlanTime()) >= 0) {
                    log.info("该轮群发完成");
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200).setResult("群发完成").setRealTime(LocalDateTime.now()));
                }*/
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            log.info("异常信息{}", ExceptionUtil.getCausedBy(e));
            return true;
        }

    }

}
