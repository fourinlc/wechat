package com.xxx.server.redis;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.AllArgsConstructor;
import org.assertj.core.util.Lists;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 异步同步redis聊天消息
 */
@Component
@AllArgsConstructor
public class AsyncGroupLinkDetail implements CommandLineRunner {

    private RedisTemplate<String,Object> redisTemplate;

    private static final String SUFFIX = "_wx_sync_msg_topic";

    private static final String MSGS = "AddMsgs";

    private IWeixinBaseInfoService weixinBaseInfoService;

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @Override
    public void run(String... args) throws Exception {
        // 获取所有存活微信信息
        Set<String> stringSet = RedisHelper.scan(redisTemplate, "*" + SUFFIX);
        // 获取对应的uuid,验证其登录情况
        List<WeixinGroupLinkDetail> datas = new LinkedList<>();
        // 提取微信id列表
        List<WeixinBaseInfo> weixinBaseInfos = Lists.newArrayList();
        for (String uuidTopic : stringSet) {
            // 截取对应的uuid信息
            String uuid = uuidTopic.replaceAll(SUFFIX, "");
            // 获取对应的在线微信
            Object o = redisTemplate.opsForValue().get(uuid);
            if(o instanceof JSONObject){
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
                while (true){
                    // 100毫秒没取出来，跳出循环
                    Object data = redisTemplate.opsForList().leftPop(uuidTopic, 100, TimeUnit.MILLISECONDS);
                    if(Objects.isNull(data)) break;
                    if(data instanceof JSONObject){
                        // 校验数据是否是我们需要的类型
                        JSONArray jsonArray = ((JSONObject) data).getJSONArray(MSGS);
                        // 通常只有一条信息
                        for (Object o1 : jsonArray) {
                            JSONObject jsonObject = (JSONObject) o1;
                            // 获取发送人信息，判断是否为群消息，群消息过滤掉 from_user_name字段校验
                            JSONObject fromUserNameVO = jsonObject.getJSONObject("from_user_name");
                            String fromUserName = fromUserNameVO.getString("str");
                            if(StrUtil.contains(fromUserName, "@chatroom")){
                                continue;
                            }
                            jsonObject.put("from_user_name", fromUserName);
                            JSONObject toUserNameVO = jsonObject.getJSONObject("to_user_name");
                            String toUserName = toUserNameVO.getString("str");
                            jsonObject.put("to_user_name", toUserName);
                            // content 消息内容
                            // create_time 消息创建时间
                            // 原始消息msg_id信息
                            WeixinGroupLinkDetail weixinGroupLinkDetail = jsonObject.to(WeixinGroupLinkDetail.class);
                            datas.add(weixinGroupLinkDetail);
                        }
                    }
                }
            }

        }

        // 异步批量更新至在线列表数据
        weixinBaseInfoService.saveOrUpdateBatch(weixinBaseInfos);
        // 处理所有消息列表数据
        WeixinGroupLinkDetail tmp = new WeixinGroupLinkDetail();
        for (WeixinGroupLinkDetail data : datas) {
            // 处理具体数据，每两条数据为一组，可能还得剔除部分无效数据,msg_type为49的消息即为群邀请操作
            Integer msgType = data.getMsgType();
            if(msgType == 49){
                // 判断上一组数据是否入库
                if (tmp.getRemark().isEmpty()) {
                    // 直接入库
                    WeixinGroupLinkDetail weixinGroupLinkDetail = tmp.clone();
                    datas.add(weixinGroupLinkDetail);
                }
                // 直接入库至本地数据
                tmp = data;
            }else {
                //TODO 存在首个数据就是上一个的备注信息
                // 提取备注信息
                String content = data.getContent();
                tmp.setRemark(content);
                WeixinGroupLinkDetail weixinGroupLinkDetail = tmp.clone();
                datas.add(weixinGroupLinkDetail);
            }
        }
        // 批量更新入库
        weixinGroupLinkDetailService.saveOrUpdateBatch(datas);
    }
}

