package com.xxx.server.mq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component("groupChatTag")
@Slf4j
public class GroupWeChatNewMqMessageHandler implements MqMessageHandler {

    @Resource
    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @Resource
    private IWeixinTemplateDetailService weixinTemplateDetailService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private IWeixinBaseInfoService weixinBaseInfoService;

    @Resource
    private IWeixinTemplateSendDetailService weixinTemplateSendDetailService;

    @Resource
    private IWeixinTemplateService weixinTemplateService;

    @Resource
    private IWeixinDictionaryService weixinDictionaryService;

    @Value("${wechat.file.basePath}")
    private String basePath;

    @Override
    public boolean process(JSONObject message) {
        try {
            log.info("开始处理批量拉群操作=======");
            Long asyncEventCallId = message.getLong("asyncEventCallId");
            String chatRoomName = message.getString("chatRoomName");
            WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);

            if (Objects.isNull(weixinAsyncEventCall) || StrUtil.isEmpty(chatRoomName)) {
                log.info("数据格式不正确,忽略该数据流程提前结束:{}", message);
                return true;
            }
            if(weixinAsyncEventCall.getResultCode() == 200){
                log.info("流程已结束，无须处理");
                return true;
            }
            WeixinTemplateSendDetail weixinTemplateSendDetail = weixinTemplateSendDetailService.getOne(Wrappers.lambdaQuery(WeixinTemplateSendDetail.class)
                    .eq(WeixinTemplateSendDetail::getAsyncEventCallId, weixinAsyncEventCall.getAsyncEventCallId())
                    .eq(WeixinTemplateSendDetail::getWxId, weixinAsyncEventCall.getWxId())
                    .eq(WeixinTemplateSendDetail::getChatRoomId, chatRoomName));
            if (weixinAsyncEventCall.getResultCode() == 500) {
                log.info("数据格式不正确,忽略该数据流程提前结束:{}", message);
                // 更新话术详情
                if (weixinTemplateSendDetail != null) {
                    weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult(weixinAsyncEventCall.getResult()));
                }
                return true;
            }
            JSONObject templateIds = message.getJSONObject("templateIds");
            // 验证小号是否存在群中，通过主号获取群成员列表方式
            JSONObject jsonObject = JSONObject.of("ChatRoomName", chatRoomName);
            MultiValueMap<String, String> queryBase = new LinkedMultiValueMap<>();
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            // 获取对应的key值
            String wxId = weixinAsyncEventCall.getWxId();
            // 先获取对应的子账号列表
            String wxIdA = message.getString("wxIdA");
            String wxIdB = message.getString("wxIdB");
            // 检查两个账号是否都在群中
            WeixinBaseInfo weixinBaseInfoA = weixinBaseInfoService.getById(wxIdA);
            WeixinBaseInfo weixinBaseInfoB = weixinBaseInfoService.getById(wxIdB);
            String keyA = weixinBaseInfoA.getKey();
            String keyB = weixinBaseInfoB.getKey();
            List<String> wxIds = Lists.newArrayList(wxIdA, wxIdB);
            List<String> keys = Lists.newArrayList(keyB, keyA);
            long countVo = 0;
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                queryBase.clear();
                queryBase.add("key", key);
                JSONObject chatroomMemberDetailVo = WechatApiHelper.GET_CHATROOM_MEMBER_DETAIL.invoke(jsonObject, queryBase);
                if (ResConstant.CODE_SUCCESS.equals(chatroomMemberDetailVo.getInteger(ResConstant.CODE))) {
                    // 自己存在
                    // 获取群头像信息
                    JSONArray memberDatasVo = chatroomMemberDetailVo.getJSONObject(ResConstant.DATA).getJSONObject("member_data").getJSONArray("chatroom_member_list");
                    if(memberDatasVo == null){
                        log.info("该账号登录状态不在这个群内key:{}", key);
                        continue;
                    }
                    // 先获取对应的子账号列表
                    countVo = memberDatasVo.stream().filter(o -> {
                        Map map = (Map) o;
                        JSONObject jsonObject1 = new JSONObject(map);
                        return wxIds.contains(jsonObject1.getString("user_name"));
                    }).count();
                }
                if (countVo > 0) break;
            }
            log.info("开始获取配置信息");
            List<WeixinDictionary> scanIntoUrlGroupTimes = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicCode("groupChat"));
            // Assert.isTrue(scanIntoUrlGroupTimes.size() >= 2, "系统进群消息配置异常");
            // 获取对应随机数字1-5, 默认2-4秒
            JSONObject dices = new JSONObject();
            scanIntoUrlGroupTimes.forEach(scanIntoUrlGroupTime -> {
                dices.put(scanIntoUrlGroupTime.getDicKey(), scanIntoUrlGroupTime.getDicValue());
            });
            // 增加缓存信息
            int max = dices.getIntValue("msg_mass_max", 1500);
            int min = dices.getIntValue("msg_mass_min", 1000);
            log.info("群发群间模板隔配置时间min:{},max:{}", min, max);
            if(max < min){
                log.info("配置信息异常");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("群消息发送配置最大值最小值有误").setRealTime(LocalDateTime.now()));
                return true;
            }
            // 校验这两个子账号是否还在群聊中
            if (countVo == 0) {
                log.info("该群不包含子账户信息：{} {}", wxId, chatRoomName);
                weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("该群不包含子账户信息"));
                return true;
            }
            String type = countVo == 1 ? "single" : "double";
            List<Long> types = templateIds.getList(type, Long.class);
            // 双人模板话术
            // 维护至redis中，标识当前微信执行到的模板次序
            Integer count = (Integer) redisTemplate.opsForValue().get("count::" + type + wxId);
            if (count == null) count = 0;
            // 校验该批次是否还是有些状态
            // 获取对应文件信息
            log.info("step two wxId : {} 模板方式：{}", wxId, type);
            List<WeixinTemplateDetail> list = weixinTemplateDetailService.list(Wrappers.lambdaQuery(WeixinTemplateDetail.class).in(WeixinTemplateDetail::getTemplateId, types));
            // 对具体模板进行分组
            Map<Long, List<WeixinTemplateDetail>> WeixinTemplateDetailMap = list.stream().collect(Collectors.groupingBy(WeixinTemplateDetail::getTemplateId));
            Collection<List<WeixinTemplateDetail>> values = WeixinTemplateDetailMap.values();
            List<List<WeixinTemplateDetail>> lists = Lists.newArrayList(values);
            // step one 遍历模板列表
            List<WeixinTemplateDetail> weixinTemplateDetails = lists.get(count % lists.size());
            // 获取模板id和模板名称
            WeixinTemplate weixinTemplate = new WeixinTemplate();
            if (weixinTemplateDetails.size() > 0) {
                WeixinTemplateDetail weixinTemplateDetail1 = weixinTemplateDetails.get(0);
                weixinTemplate = weixinTemplateService.getById(weixinTemplateDetail1.getTemplateId());
                log.info("当前具体模板信息：{}", weixinTemplate);
            }
            log.info("当前count值：{}", count);
            // 每隔两秒执行一次
            for (int i = 0; i < weixinTemplateDetails.size(); i++) {
                log.info("执行批次：{}", i);
                List<JSONObject> jsonObjectList = Lists.newArrayList();
                JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
                jsonObjectList.add(param);
                JSONObject paramVo = JSONObject.of("MsgItem", jsonObjectList);
                WeixinTemplateDetail weixinTemplateDetail = weixinTemplateDetails.get(i);
                // 构造模板参数
                String key = "A".equals(weixinTemplateDetail.getMsgRole()) ? keyA : keyB;
                query.add("key", key);
                // 查询当前号码是还在否在群内,还是通过主账号查询
                if (i > 0) {
                    // 获取子号是否还在群内
                    queryBase.clear();
                    queryBase.add("key", key);
                    JSONObject chatroomMemberDetailVo = WechatApiHelper.GET_CHATROOM_MEMBER_DETAIL.invoke(jsonObject, queryBase);
                    if (!ResConstant.CODE_SUCCESS.equals(chatroomMemberDetailVo.getInteger(ResConstant.CODE))) {
                        log.error("子账号二次校验时异常 key:{}, 结束本轮操作", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("子账号二次校验时异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("子账号二次校验时异常").setRealTime(LocalDateTime.now()));
                        return true;
                    }
                }
                // 从缓存中获取随机间隔时间缓存
                List<WeixinDictionary> weixinDictionaries = weixinDictionaryService.query(new WeixinDictionary().setDicGroup("system").setDicKey("groupChat"));
                // 获取随机间隔时间最大值和最小值

                // 1默认为普通文字消息
                if ("1".equals(weixinTemplateDetail.getMsgType())) {
                    param.put("AtWxIDList", null);
                    param.put("MsgType", 1);
                    param.put("TextContent", weixinTemplateDetail.getMsgContent() + "执行时间："+DateUtil.formatDateTime(new Date()));
                    // 发送文字信息
                    JSONObject textMessage = WechatApiHelper.SEND_TEXT_MESSAGE.invoke(paramVo, query);
                    log.info("step four 发送文本消息：当前时间：{}，发送内容：{}， 返回值：{}，", new Date(), weixinTemplateDetail.getMsgContent(), textMessage);
                    if (!ResConstant.CODE_SUCCESS.equals(textMessage.getInteger(ResConstant.CODE))) {
                        log.error("子账号发送文本信息异常 key:{}, 终止整个流程", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("子账号发送文本信息异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("子账号发送文本信息异常key:" + key).setRealTime(LocalDateTime.now()));
                        return true;
                    } else {
                        // 随机延时两秒进行下一个话术操作
                        try {
                            int i1 = RandomUtil.randomInt(min, max);
                            log.info("下次延时时间为{}ms", i1);
                            Thread.sleep(i1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    paramVo = JSONObject.of("MsgItem", jsonObjectList);
                    param.put("TextContent", "");
                    // 获取图片信息用于展示
                    try {
                        File file = new File(basePath + weixinTemplateDetail.getMsgContent());
                        param.put("ImageContent", FileUtil.readBytes(file));
                    }catch (Exception e){
                        paramVo.clear();
                        query.clear();
                        log.error("图片获取异常，跳过本次发送");
                        continue;
                    }
                    JSONObject imageMessage = WechatApiHelper.SEND_IMAGE_MESSAGE.invoke(paramVo, query);
                    if (!ResConstant.CODE_SUCCESS.equals(imageMessage.getInteger(ResConstant.CODE))) {
                        log.error("子账号发送图片信息异常 key:{}, 终止整个流程", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("子账号发送图片信息异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("子账号发送图片信息异常key:" + key).setRealTime(LocalDateTime.now()));
                        // 重置计数器模板计数器
                        return true;
                    } else {
                        // 随机延时两秒进行下一个话术操作
                        try {
                            Thread.sleep(RandomUtil.randomInt(min, max));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // 清空param、query参数
                paramVo.clear();
                query.clear();
            }
            // 未出现异常时将群模板顺序移动至下一个节点
            redisTemplate.opsForValue().set("count::" + type + wxId, ++count);
            weixinTemplateSendDetailService.updateById(
                    weixinTemplateSendDetail
                            .setTemplateId(weixinTemplate.getTemplateId())
                            .setFinishTime(LocalDateTime.now())
                            .setStatus("200")
                            .setResult("群发完成"));
            // 当前时间大于计划完成时间
            if (LocalDateTime.now().compareTo(weixinAsyncEventCall.getPlanTime()) >= 0) {
                log.info("该轮群发完成");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200).setResult("群发完成").setRealTime(LocalDateTime.now()));
                // 重置count
                redisTemplate.opsForValue().set("count::double" + wxId, 0);
                redisTemplate.opsForValue().set("count::single" + wxId, 0);
                return true;
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return true;
        }

    }

}
