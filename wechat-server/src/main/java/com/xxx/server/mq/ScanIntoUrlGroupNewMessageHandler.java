package com.xxx.server.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
            log.info("1、开始群链接进群");
            long start = System.currentTimeMillis();
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
            // 操作群链接对应的id
            Long linkId = message.getLong("linkId");
            // 批量拉群特有字段
            WeixinGroupLinkDetail weixinGroupLinkDetail;
            Long asyncEventCallId = message.getLong("asyncEventCallId");
            // 子号信息
            List<String> wxIds = message.getList("wxIds", String.class);
            WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
            if (Objects.isNull(weixinAsyncEventCall)  || weixinAsyncEventCall.getResultCode() == 500) {
                log.info("流程提前结束：{}", message);
                // 更新原始数据进群详情信息,增加描述信息
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("流程提前结束"));
                return true;
            }
            if(linkId == null){
                log.info("2、批量拉群自动进群方式开始");
                // 自动进群操作
                JSONObject paramVo = message.getJSONObject("paramVo");
                String toUserWxId = paramVo.getString("toUserWxId");
                String fromUserWxId = paramVo.getString("fromUserWxId");
                String chatroomName = paramVo.getString("chatroomName");
                List<WeixinGroupLinkDetail> weixinGroupLinkDetails = weixinGroupLinkDetailService.list(Wrappers.lambdaQuery(WeixinGroupLinkDetail.class)
                        .eq(WeixinGroupLinkDetail::getToUserWxId, toUserWxId)
                        .eq(WeixinGroupLinkDetail::getFromUserWxId, fromUserWxId)
                        .eq(WeixinGroupLinkDetail::getChatroomName, chatroomName)
                        .eq(WeixinGroupLinkDetail::getLinkStatus, 0));
                if(weixinGroupLinkDetails.size() > 0){
                    weixinGroupLinkDetail = weixinGroupLinkDetails.get(0);
                    Long groupSendDetailId = message.getLong("groupSendDetailId");
                    // 批量拉群特殊标识字段
                    weixinGroupLinkDetail.setGroupSendDetailId(groupSendDetailId);
                }else {
                    // 结束自动进群
                    log.info("获取群链接失败");
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("获取群链接失败"));
                    return true;
                }
            }else {
                log.info("2、普通链接进群");
                weixinGroupLinkDetail = weixinGroupLinkDetailService.getById(linkId);
            }
            if(weixinGroupLinkDetail == null){
                log.info("获取群链接失败");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("获取群链接失败"));
                return true;
            }
            // Date delay = new Date();
            // 校验该批次是否还是有些状态
            String toUserWxId = weixinGroupLinkDetail.getToUserWxId();
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(toUserWxId);
            // 获取对应的key值
            if (weixinBaseInfo == null || !StrUtil.equals(weixinBaseInfo.getState(), "1")) {
                return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "被邀请号不在线", start);
            }
            multiValueMap.add("key", weixinBaseInfo.getKey());
            // 增加验证邀请人是否还是主号好友验证，如果不是结束流程，当然排除拉群的
            JSONObject friendRelation = WechatApiHelper.GET_FRIEND_RELATION.invoke(JSONObject.of("UserName", weixinGroupLinkDetail.getFromUserWxId()), multiValueMap);
            if (!(ResConstant.CODE_SUCCESS.equals(friendRelation.getInteger(ResConstant.CODE)))) {
               // 异常状态
                return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "好友关系判断异常", start);
            }
            //判断好友关系，1//删除 4/自己拉黑 5/被拉黑 0/正常
            Integer friendRelationVo = friendRelation.getJSONObject(ResConstant.DATA).getInteger("FriendRelation");
            log.info("3、校验好友关系,返回值：{}", friendRelationVo);
            if(friendRelationVo != null && (friendRelationVo.equals(1) || friendRelationVo.equals(5))){
                return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "好友关系判断异常", start);
            }
            try {
                // 随机休眠0.5-1秒
                Thread.sleep(RandomUtil.randomInt(500, 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 额外校验群链接生效情况
            JSONObject data = WechatApiHelper.SCAN_INTO_URL_GROUP.invoke(JSONObject.of("Url", weixinGroupLinkDetail.getContent()), multiValueMap);
            log.info("4、开始群链接进群,返回值：{}", data);
            if (!(ResConstant.CODE_SUCCESS.equals(data.getInteger(ResConstant.CODE)) && "进群成功".equals(data.getString("Text")))) {
                return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "群链接进群失败", start);
            }
            // 更新链接状态为进群完成
            weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setLinkStatus("1"));
            JSONObject jsonObject = data.getJSONObject(ResConstant.DATA);
            String chatroomUrl = jsonObject.getString("chatroomUrl");
            Matcher matcher = Pattern.compile(ResConstant.PATTERN).matcher(chatroomUrl);
            if (matcher.find()) {
                String chatroomName = matcher.group();
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
                    return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "主号已被踢", start);
                }
                // 提取对应的群验证标识字段，并更新至邀请链接中
                JSONObject contact = chatRoomInfoRes.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
                log.info("5、校验是否还在群中同时更新群是否验证标识");
                String chatroomAccessType = contact.getString("chatroomAccessType");
                // 更新群验证状态
                weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setVerifyStatus(chatroomAccessType));
                // 开始组装保存群聊
                // 进群成功后，保存群聊，更新邀请链接状态
                // 延迟2-3秒，开始保存群聊信息，组装对应参数，获取群id
                JSONObject jsonObject1 = JSONObject.of("ChatRoomName", chatroomName, "Val", 1);
                // 随机延时1-2操作
                try {
                    // 随机休眠1-2秒
                    Thread.sleep(RandomUtil.randomInt(500, 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JSONObject movetoContract = WechatApiHelper.MOVETO_CONTRACT.invoke(jsonObject1, multiValueMap);
                log.info("6、保存群聊信息返回{}", movetoContract);
                if (!ResConstant.CODE_SUCCESS.equals(movetoContract.getInteger(ResConstant.CODE))) {
                    return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "保存群聊失败", start);
                }
                // 更新系统状态批次状态
                // 更新链接状态为保存群聊消息
                weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setLinkStatus("2").setResult("保存群聊成功"));
                // 保存群聊成功时，邀请两个子号进行进群操作
                try {
                    Thread.sleep(RandomUtil.randomInt(1000, 1500));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 获取子账号信息，如果不存在，则无须邀请进群了
                if (!(wxIds == null || wxIds.isEmpty())) {
                    log.info("7、开始子号进群");
                    JSONObject jsonObject2 = JSONObject.of("ChatRoomName", chatroomName, "UserList", wxIds);
                    // TODO 判断群是否是验证群，验证群跳过,或者是不是好友关系跳过
                    JSONObject addChatroomMembers = WechatApiHelper.INVITE_CHATROOM_MEMBERS.invoke(jsonObject2, multiValueMap);
                    if (!ResConstant.CODE_SUCCESS.equals(addChatroomMembers.getInteger(ResConstant.CODE))) {
                        return writeLog(weixinGroupLinkDetail, weixinAsyncEventCall, "邀请子号进群失败", start);
                    }
                    weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setLinkStatus("4").setResult("进群成功").setLinkStatus("500"));
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200));
                }else {
                    log.info("7、没有子号进群，流程结束");
                    // 更新群聊消息状态
                    weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setLinkStatus("4").setResult("进群成功"));
                    weixinAsyncEventCall.setResultCode(200);
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
                }
                if (LocalDateTime.now().compareTo(weixinAsyncEventCall.getPlanTime()) >= 0) {
                    log.info("该轮进群拉群完成");
                    weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200).setResult("进群完成").setRealTime(LocalDateTime.now()));
                }
            }
            log.info("8、共耗费时间：{} ms", System.currentTimeMillis() - start);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            log.info("异常信息{}", ExceptionUtil.getCausedBy(e));
            return true;
        }

    }

    private boolean writeLog(WeixinGroupLinkDetail weixinGroupLinkDetail, WeixinAsyncEventCall weixinAsyncEventCall, String msg, long start) {
        log.info(msg);
        weixinGroupLinkDetailService.updateById(weixinGroupLinkDetail.setResult(msg).setLinkStatus("500"));
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult(msg));
        log.info("异常结束：耗时{}", System.currentTimeMillis() -start);
        return true;
    }

}
