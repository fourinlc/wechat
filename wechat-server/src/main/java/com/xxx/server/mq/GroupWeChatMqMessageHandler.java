package com.xxx.server.mq;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.pojo.WeixinTemplateDetail;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinRelatedContactsService;
import com.xxx.server.service.IWeixinTemplateDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("groupChatTag")
@Slf4j
@AllArgsConstructor
public class GroupWeChatMqMessageHandler implements MqMessageHandler {

    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    private IWeixinTemplateDetailService weixinTemplateDetailService;

    private RedisTemplate<String, Object> redisTemplate;

    private IWeixinBaseInfoService weixinBaseInfoService;

    private IWeixinRelatedContactsService weixinRelatedContactsService;

    @Override
    public boolean process(JSONObject message) {
        Long asyncEventCallId = message.getLong("asyncEventCallId");
        String chatRoomName = message.getString("chatRoomName");
        WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
        if (Objects.isNull(weixinAsyncEventCall) || weixinAsyncEventCall.getResultCode() == 500 || StrUtil.isEmpty(chatRoomName)) {
            log.info("数据格式不正确,忽略该数据流程提前结束:{}", message);
            return true;
        }
        List<Long> templateIds = message.getList("templateIds", Long.class);
        // 验证小号是否存在群中，通过主号获取群成员列表方式
        // TODO 增加校验该用户是否还在群中，从获取联系人列表查看自己是否在这个群里
        JSONObject jsonObject = JSONObject.of("ChatRoomName", chatRoomName);
        MultiValueMap<String, String> queryBase = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        // 获取对应的key值
        String wxId = weixinAsyncEventCall.getWxId();
        // 通过wxId获取对应的key值
        WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(wxId);
        if (weixinBaseInfo == null || StrUtil.isEmpty(weixinBaseInfo.getKey())) {
            log.error("该微信信息不存在wxId：{}", wxId);
            return true;
        }
        queryBase.add("key", weixinBaseInfo.getKey());
        log.info("step one 首次校验子账号是否进群：{}", wxId);
        JSONObject chatroomMemberDetail = WechatApiHelper.GET_CHATROOM_MEMBER_DETAIL.invoke(jsonObject, queryBase);
        if (ResConstant.CODE_SUCCESS.equals(chatroomMemberDetail.getInteger(ResConstant.CODE))) {
            JSONArray memberDatas = chatroomMemberDetail.getJSONObject(ResConstant.DATA).getJSONObject("member_data").getJSONArray("chatroom_member_list");
            // 先获取对应的子账号列表
            WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getById(wxId);
            String wxIdA = weixinRelatedContacts.getRelated1();
            String wxIdB = weixinRelatedContacts.getRelated2();
            // 获取对应的key值
            WeixinBaseInfo weixinBaseInfoA = weixinBaseInfoService.getById(wxIdA);
            if (weixinBaseInfoA == null || StrUtil.isEmpty(weixinBaseInfoA.getKey())) {
                log.error("该微信信息不存在wxIdA：{}", wxIdA);
                return true;
            }
            String keyA = weixinBaseInfoA.getKey();
            WeixinBaseInfo weixinBaseInfoB = weixinBaseInfoService.getById(wxIdB);
            if (weixinBaseInfoB == null || StrUtil.isEmpty(weixinBaseInfoB.getKey())) {
                log.error("该微信信息不存在wxIdB：{}", wxIdB);
                return true;
            }
            String keyB = weixinBaseInfoB.getKey();
            List<String> wxIds = Lists.newArrayList(wxIdA, wxIdB);
            // 校验这两个子账号是否还在群聊中
            List<Object> userNames = memberDatas.stream().filter(o -> {
                Map map = (Map) o;
                JSONObject jsonObject1 = new JSONObject(map);
                return wxIds.contains(jsonObject1.getString("user_name"));
            }).collect(Collectors.toList());
            if (userNames.size() == 1) {
                log.info("step two 单模板方式：{}", wxId);
                // 单模板话术
                JSONObject o = (JSONObject) userNames.get(0);
                String key = StrUtil.equals(o.getString("user_name"), wxIdA) ? keyA : keyB;
                // TODO 使用单模板话术进行操作

            } else if (userNames.size() == 2) {
                // 双人模板话术
                // 维护至redis中，标识当前微信执行到的模板次序
                Integer count = (Integer)redisTemplate.opsForValue().get( "count :: " + wxId);
                if(count == null){
                    // 设置初始值
                    count = 0;
                }
                // 校验该批次是否还是有些状态
                // 获取对应文件信息
                log.info("step two 双人模板方式：{}", wxId);
                List<WeixinTemplateDetail> list = weixinTemplateDetailService.list(Wrappers.lambdaQuery(WeixinTemplateDetail.class).in(WeixinTemplateDetail::getTemplateId, templateIds));
                // 对具体模板进行分组
                Map<Long, List<WeixinTemplateDetail>> WeixinTemplateDetailMap = list.stream().collect(Collectors.groupingBy(WeixinTemplateDetail::getTemplateId));
                Collection<List<WeixinTemplateDetail>> values = WeixinTemplateDetailMap.values();
                List<List<WeixinTemplateDetail>> lists = Lists.newArrayList(values);
                // step one 遍历模板列表
                List<WeixinTemplateDetail> weixinTemplateDetails = lists.get(count % lists.size());

                // 每隔两秒执行一次
                for (int i = 0; i < weixinTemplateDetails.size(); i++) {
                    List<JSONObject> jsonObjectList = Lists.newArrayList();
                    JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
                    jsonObjectList.add(param);
                    JSONObject paramVo = JSONObject.of("MsgItem", jsonObjectList);
                    WeixinTemplateDetail weixinTemplateDetail = weixinTemplateDetails.get(i);
                    // 构造模板参数
                    query.add("key", "A".equals(weixinTemplateDetail.getMsgRole()) ? keyA : keyB);
                    // 查询当前号码是还在否在群内,还是通过主账号查询
                    if(i > 0){
                        JSONObject chatroomMemberDetailVo = WechatApiHelper.GET_CHATROOM_MEMBER_DETAIL.invoke(jsonObject, queryBase);
                        if (ResConstant.CODE_SUCCESS.equals(chatroomMemberDetailVo.getInteger(ResConstant.CODE))) {
                            JSONArray memberDatasVo = chatroomMemberDetailVo.getJSONObject(ResConstant.DATA).getJSONObject("member_data").getJSONArray("chatroom_member_list");
                            // 先获取对应的子账号列表
                            String userName = "A".equals(weixinTemplateDetail.getMsgRole()) ? wxIdA : wxIdB;
                            long countVo = memberDatasVo.stream().filter(o -> {
                                Map map = (Map) o;
                                JSONObject jsonObject1 = new JSONObject(map);
                                return userName.equals(jsonObject1.getString("user_name"));
                            }).count();
                            log.info("step three 再次校验子账号是否还在群内：{}", wxId);
                            if(countVo == 0){
                                // 结束本轮操作了
                                return true;
                            }
                        }
                    }
                    // 1默认为普通文字消息
                    if ("1".equals(weixinTemplateDetail.getMsgType())) {
                        param.put("AtWxIDList", null);
                        param.put("MsgType", 1);
                        param.put("TextContent", weixinTemplateDetail.getMsgContent());
                        // 发送文字信息
                        JSONObject textMessage = WechatApiHelper.SEND_TEXT_MESSAGE.invoke(paramVo, query);
                        log.info("step four 发送文本消息：{}", textMessage);
                        if (!ResConstant.CODE_SUCCESS.equals(textMessage.getInteger(ResConstant.CODE))) {
                            // 发送消息失败，更新队列状态为失败，终止整个流程
                            weixinAsyncEventCall.setResultCode(500).setRealTime(LocalDateTime.now()).setResult("发送消息失败");
                            weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
                            return true;
                        } else {
                            // 随机延时两秒进行下一个话术操作
                            try {
                                Thread.sleep(RandomUtil.randomInt(1000, 1500));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        jsonObjectList.add(param);
                        paramVo = JSONObject.of("MsgItem", jsonObjectList);
                        param.put("TextContent", "");
                        // 获取图片信息用于展示
                        param.put("ImageContent", weixinTemplateDetail.getMsgContent());
                        JSONObject imageMessage = WechatApiHelper.SEND_IMAGE_MESSAGE.invoke(paramVo, query);
                        if (!ResConstant.CODE_SUCCESS.equals(imageMessage.getInteger(ResConstant.CODE))) {
                            // 发送消息失败，更新队列状态为失败，终止整个流程
                            weixinAsyncEventCall.setResultCode(500).setRealTime(LocalDateTime.now()).setResult("进群操作失败");
                            weixinAsyncEventCallService.updateById(weixinAsyncEventCall);
                            return true;
                        } else {
                            // 随机延时两秒进行下一个话术操作
                            try {
                                Thread.sleep(RandomUtil.randomInt(1000, 1500));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // 清空param、query参数
                    paramVo.clear();
                    query.clear();
                    redisTemplate.opsForValue().set( "count :: " + wxId, ++ count);
                }
                return true;
            }
        }
        // 异常状态
        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("群发失败"));
        return true;
    }
}
