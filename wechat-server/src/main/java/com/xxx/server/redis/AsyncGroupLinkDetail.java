package com.xxx.server.redis;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 异步同步redis聊天消息
 */
/*@Component*/
@AllArgsConstructor
@Slf4j
public class AsyncGroupLinkDetail implements CommandLineRunner {

    private RedisTemplate<String, Object> redisTemplate;

    private static final String SUFFIX = "_wx_sync_msg_topic";

    private static final String MSGS = "AddMsgs";

    private IWeixinBaseInfoService weixinBaseInfoService;

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @Override
    public void run(String... args) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                log.debug("开始定时处理群链接信息和在线好友信息");
                // 获取所有存活微信信息
                // Set<String> stringSet = redisTemplate.keys("*" + SUFFIX);
                Set<String> stringSet = RedisHelper.scan(redisTemplate, "*" + SUFFIX);
                // 获取对应的uuid,验证其登录情况
                List<WeixinGroupLinkDetail> datas = new LinkedList<>();
                // 提取微信id列表
                List<WeixinBaseInfo> weixinBaseInfos = Lists.newArrayList();
                log.debug("待处理消息列表：{}", stringSet);
                for (String uuidTopic : stringSet) {
                    // 截取对应的uuid信息
                    String uuid = uuidTopic.replaceAll(SUFFIX, "");
                    // 获取对应的在线微信
                    Object o = redisTemplate.opsForValue().get(uuid);
                    if (o instanceof JSONObject) {
                        // 存在异常登录状态或者状态不为1即为异常
                        if (StrUtil.isNotEmpty(((JSONObject) o).getString("ErrMsg")) || ((JSONObject) o).getLong("state") != 1) {
                            // 账户已退出或者异常状态
                            continue;
                        }
                        WeixinBaseInfo weixinBaseInfo = ((JSONObject) o).to(WeixinBaseInfo.class);
                        weixinBaseInfo.setKey(weixinBaseInfo.getUuid());
                        weixinBaseInfo.setUuid(null);
                        weixinBaseInfos.add(weixinBaseInfo);
                        // 同步微信信息
                        while (true) {
                        // 100毫秒没取出来，跳出循环
                         Object data = redisTemplate.opsForList().leftPop(uuidTopic, 100, TimeUnit.MILLISECONDS);
                        // List<Object> dataVo = redisTemplate.opsForList().range(uuidTopic, 0, 14);
                        if (Objects.isNull(data)) break;
                       /* for (Object data : dataVo) {*/
                            if (data instanceof JSONObject) {
                                // 校验数据是否是我们需要的类型
                                JSONArray jsonArray = ((JSONObject) data).getJSONArray(MSGS);
                                if(jsonArray == null) continue;
                                // 通常只有一条信息
                                for (Object o1 : jsonArray) {
                                    JSONObject jsonObject = (JSONObject) o1;
                                    // 获取发送人信息，判断是否为群消息，群消息过滤掉 from_user_name字段校验
                                    JSONObject fromUserNameVO = jsonObject.getJSONObject("from_user_name");
                                    String fromUserName = fromUserNameVO.getString("str");
                                    if (StrUtil.contains(fromUserName, "@chatroom")) {
                                        continue;
                                    }
                                    jsonObject.put("from_user_name", fromUserName);
                                    JSONObject toUserNameVO = jsonObject.getJSONObject("to_user_name");
                                    String toUserName = toUserNameVO.getString("str");
                                    jsonObject.put("to_user_name", toUserName);
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
                            /*  }*/
                        }
                    }
                }

                // 异步批量更新至在线列表数据
                weixinBaseInfoService.saveOrUpdateBatch(weixinBaseInfos);
                // 处理所有消息列表数据
                WeixinGroupLinkDetail tmp = new WeixinGroupLinkDetail();
                List<WeixinGroupLinkDetail> dataVos = new LinkedList<>();
                for (int i = 0; i < datas.size(); i++) {
                    WeixinGroupLinkDetail data = datas.get(i);
                    // 未处理状态
                    data.setLinkStatus("0");
                    data.setInvitationTime(DateUtil.format(new Date(), "yyyy-mm-dd"));
                    // 处理具体数据，每两条数据为一组，可能还得剔除部分无效数据,msg_type为49的消息即为群邀请操作
                    Integer msgType = data.getMsgType();
                    if (msgType == 49) {
                        // 转义获取群链接地址
                        String url = data.getContent();
                        JSONObject jsonObject = buildUrl(url);
                        data.setContent(jsonObject.getString("url"));
                        data.setChatroomName(jsonObject.getString("title"));
                        // 判断上一组数据是否入库,群链接信息有了
                        if (StrUtil.isEmpty(tmp.getRemark()) && StrUtil.isNotEmpty(tmp.getContent())) {
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
                        String content = data.getContent();
                        tmp.setRemark(content);
                        WeixinGroupLinkDetail weixinGroupLinkDetail = tmp.clone();
                        dataVos.add(weixinGroupLinkDetail);
                    }
                }
                // 批量更新入库
                weixinGroupLinkDetailService.saveOrUpdateBatch(dataVos);
            }
        };
        // 每1分钟执行一次
        timer.schedule(timerTask, 0, 1 * 60 * 1000);
    }

    // 提取群链接地址
    private JSONObject buildUrl(String url) {
        try {
            log.info("打印群链接信息：{}", url);
            Document document = DocumentHelper.parseText(url);
            Node node = document.selectSingleNode("/msg/appmsg/url");
            String title = document.selectSingleNode("/msg/appmsg/des").getText();
            return JSONObject.of("url", node.getText(), "title", title);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}

