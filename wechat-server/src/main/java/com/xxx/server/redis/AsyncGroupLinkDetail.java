package com.xxx.server.redis;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步同步redis聊天消息
 */
@Component
@AllArgsConstructor
@Slf4j
public class AsyncGroupLinkDetail implements CommandLineRunner {

    private IWeixinBaseInfoService weixinBaseInfoService;

    private RedisTemplate<String, Object> redisTemplate;

    private static final String SUFFIX = "_wx_sync_msg_topic";

    private static final String MSGS = "AddMsgs";

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    private static final List<String> SYSTEM_MESSAGE = Lists.newArrayList("newsapp", "weixin", "fmessage");

    @Override
    public void run(String... args) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                log.debug("开始定时处理群链接信息和在线好友信息");
                // 获取所有存活微信信息
                // Set<String> stringSet = redisTemplate.keys("*" + SUFFIX);
                Set<String> stringSet = RedisHelper.scan(redisTemplate, "*");
                // 获取对应的uuid,验证其登录情况
                List<WeixinGroupLinkDetail> datas = new LinkedList<>();
                // 提取微信id列表
                List<WeixinBaseInfo> weixinBaseInfos = Lists.newArrayList();
                log.debug("待处理消息列表：{}", stringSet);
                for (String uuidTopic : stringSet) {
                    // 剔除loginLog结尾参数
                    if (uuidTopic.endsWith("loginLog") || uuidTopic.startsWith("dic") || uuidTopic.startsWith("wechat")) {
                        continue;
                    } else if (uuidTopic.endsWith(SUFFIX)) {
                        // 循环处理消息
                        while (true) {
                            // 100毫秒没取出来，跳出循环
                            Object data = redisTemplate.opsForList().leftPop(uuidTopic, 100, TimeUnit.MILLISECONDS);
                            // List<Object> dataVo = redisTemplate.opsForList().range(uuidTopic, 0, 14);
                            if (Objects.isNull(data)) break;
                            /* for (Object data : dataVo) {*/
                            if (data instanceof JSONObject) {
                                // 校验数据是否是我们需要的类型
                                JSONArray jsonArray = ((JSONObject) data).getJSONArray(MSGS);
                                if (jsonArray == null) continue;
                                // 通常只有一条信息
                                for (Object o1 : jsonArray) {
                                    JSONObject jsonObject = (JSONObject) o1;
                                    // 获取发送人信息，判断是否为群消息，群消息过滤掉 from_user_name字段校验
                                    JSONObject fromUserNameVO = jsonObject.getJSONObject("from_user_name");
                                    String fromUserWxId = fromUserNameVO.getString("str");
                                    // 过滤系统消息
                                    if (SYSTEM_MESSAGE.contains(fromUserWxId) || StrUtil.contains(fromUserWxId, "@chatroom")) {
                                        continue;
                                    }
                                    if(StrUtil.equals("49", jsonObject.getString("msg_type"))){
                                        log.info(((JSONObject) data).toJSONString());
                                    }
                                    WeixinBaseInfo fromWeixinBaseInfo = weixinBaseInfoService.getById(fromUserWxId);
                                    // 这个参数只能从我的好友列表中获取对应的昵称
                                    // 如果存在直接添加至邀请连接中
                                    if (fromWeixinBaseInfo != null){
                                        // 移除自己发出去的消息
                                        if (StrUtil.equals(fromWeixinBaseInfo.getKey(), ((JSONObject) data).getString("UUID"))) {
                                            continue;
                                        }
                                        jsonObject.put("from_user_name", fromWeixinBaseInfo.getNickname());
                                    }else {
                                        // 调用微信接口
                                        JSONObject param = JSONObject.of("UserNames", Lists.newArrayList(fromUserWxId));
                                        MultiValueMap multiValueMap = new LinkedMultiValueMap();
                                        multiValueMap.add("key", ((JSONObject) data).getString("UUID"));
                                        JSONObject dataVo =  WechatApiHelper.GET_CONTACT_DETAILS_LIST.invoke(param, multiValueMap);
                                        if(ResConstant.CODE_SUCCESS.equals(dataVo.getInteger(ResConstant.CODE))){
                                            // 获取对应的微信昵称
                                            JSONObject contact = dataVo.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
                                            String string = contact.getJSONObject("nickName").getString("str");
                                            jsonObject.put("from_user_name", string);
                                        }
                                    }

                                    jsonObject.put("from_user_wxId", fromUserWxId);
                                    JSONObject toUserNameVO = jsonObject.getJSONObject("to_user_name");
                                    String toUserWxId = toUserNameVO.getString("str");
                                    jsonObject.put("to_user_wxId", toUserWxId);
                                    // 先尝试从维护的列表中获取
                                    WeixinBaseInfo toWeixinBaseInfo = weixinBaseInfoService.getById(toUserWxId);
                                    if (toWeixinBaseInfo != null){
                                        jsonObject.put("to_user_name", toWeixinBaseInfo.getNickname());
                                    }
                                    // content 消息内容
                                    JSONObject contentVo = jsonObject.getJSONObject("content");
                                    String content = contentVo.getString("str");
                                    jsonObject.put("content", content);
                                    // create_time 消息创建时间
                                    // 原始消息msg_id信息
                                    // key
                                    jsonObject.put("key", ((JSONObject) data).getString("UUID"));
                                    WeixinGroupLinkDetail weixinGroupLinkDetail = jsonObject.to(WeixinGroupLinkDetail.class);
                                    datas.add(weixinGroupLinkDetail);
                                }
                            }
                        }
                    } else {
                        // 具体微信处理
                        Object o = redisTemplate.opsForValue().get(uuidTopic);
                        if (o instanceof JSONObject) {
                            WeixinBaseInfo weixinBaseInfo = ((JSONObject) o).to(WeixinBaseInfo.class);
                            weixinBaseInfo.setKey(weixinBaseInfo.getUuid());
                            weixinBaseInfo.setUuid(null);
                            weixinBaseInfos.add(weixinBaseInfo);
                        }
                    }
                }
                List<WeixinBaseInfo> weixinBaseInfoListVo = Lists.newArrayList();
                for (List<WeixinBaseInfo> weixinBaseInfoList : weixinBaseInfos.stream().collect(Collectors.groupingBy(WeixinBaseInfo::getWxId)).values()) {
                    // 剔除相同的
                    if (weixinBaseInfoList.size() == 1) {
                        weixinBaseInfoListVo.addAll(weixinBaseInfoList);
                    }else {
                        // 获取状态为1的的数据，如果没有就取第一条数据
                        List<WeixinBaseInfo> collect = weixinBaseInfoList.stream().filter(weixinBaseInfo -> StrUtil.equals(weixinBaseInfo.getState(), "1")).collect(Collectors.toList());
                        if(collect.size() == 1) {
                            weixinBaseInfoListVo.addAll(collect);
                        }else {
                            weixinBaseInfoListVo.add(weixinBaseInfoList.get(0));
                        }
                    }
                }
                // 异步批量更新至在线列表数据
                weixinBaseInfoService.saveOrUpdateBatch(weixinBaseInfoListVo);
                // 处理所有消息列表数据
                WeixinGroupLinkDetail tmp = new WeixinGroupLinkDetail();
                List<WeixinGroupLinkDetail> dataVos = new LinkedList<>();
                for (int i = 0; i < datas.size(); i++) {
                    WeixinGroupLinkDetail data = datas.get(i);
                    // 未处理状态
                    data.setLinkStatus("0");
                    data.setInvitationTime(DateUtil.format(new Date(), "yyyy-MM-dd"));
                    // 处理具体数据，每两条数据为一组，可能还得剔除部分无效数据,msg_type为49的消息即为群邀请操作
                    Integer msgType = data.getMsgType();
                    if (msgType == 49) {
                        //log.info("接收链接类消息处理：{}", data);
                        // 转义获取群链接地址,不一定是群链接
                        String url = data.getContent();
                        JSONObject jsonObject = buildUrl(url);
                        if(jsonObject.size() == 0) continue;
                        data.setContent(jsonObject.getString("url"));
                        data.setChatroomName(jsonObject.getString("title"));
                        data.setThumbUrl(jsonObject.getString("thumbUrl"));
                        // 判断上一组数据是否入库,群链接信息有了
                        if (tmp != null && StrUtil.isEmpty(tmp.getRemark()) && StrUtil.isNotEmpty(tmp.getContent())) {
                            // 直接入库
                            WeixinGroupLinkDetail weixinGroupLinkDetail = tmp.clone();
                            dataVos.add(weixinGroupLinkDetail);
                        }
                        // 直接入库至本地数据
                        tmp = data.clone();
                        // 如果此处为最后一条信息，直接入库
                        if (datas.size() - 1 == i) {
                            dataVos.add(data);
                        }
                        // 提取备注信息,前提是这个消息类型是1,普通消息文本信息
                    } else if (msgType == 1) {
                        // 首次普通文本信息情况下，暂时过滤文本信息
                        if(tmp == null) continue;
                        String content = data.getContent();
                        tmp.setRemark(content);
                        WeixinGroupLinkDetail weixinGroupLinkDetail = tmp.clone();
                        dataVos.add(weixinGroupLinkDetail);
                    }
                }
                // 批量更新入库,手动判断入库，更新对应的
                weixinGroupLinkDetailService.saveBatch(dataVos);
            }
        };
        // 每3分钟执行一次
        timer.schedule(timerTask, 0, 3 * 60 * 1000);
    }

    // 提取群链接地址
    private JSONObject buildUrl(String url) {
        try {
            // "半勺小奶酪?"邀请你加入群聊"海娜生活超市特价公告送货群"，进入可查看详情。

            Document document = DocumentHelper.parseText(url);
            Node node = document.selectSingleNode("/msg/appmsg/url");
            String des = document.selectSingleNode("/msg/appmsg/des").getText();
            // 获取类似群id的概念
            String thumbUrl = document.selectSingleNode("/msg/appmsg/thumburl").getText();
            // 获取具体群聊名称
            List<String> roomNames = Arrays.asList(des.split("\""));
         /*   // 存在邀请人也叫xxx群情况
            if(roomNames.size() == 2){
                roomNames.remove(0);
            }*/
            String roomName = roomNames.get(3);
            return JSONObject.of("url", node.getText(), "title", roomName, "thumbUrl", thumbUrl);
        } catch (DocumentException e) {
            log.error("获取群链接失败：{}", e);
            log.info("打印群链接信息：{}", url);
        }
        return new JSONObject();
    }
}

