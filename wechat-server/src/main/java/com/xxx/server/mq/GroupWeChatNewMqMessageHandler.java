package com.xxx.server.mq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
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
import org.w3c.dom.Document;

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

    @Resource
    private IWeixinAppMessageService weixinAppMessageService;

    @Value("${wechat.file.basePath}")
    private String basePath;

    @Override
    public boolean process(JSONObject message) {
        Long count = 0L;
        Long current = 0L;
        Long currentCount = 0L;
        String wxId = "";
        WeixinAsyncEventCall weixinAsyncEventCall = new WeixinAsyncEventCall();
        try {
            log.info("1、开始处理群聊操作=======");
            Long asyncEventCallId = message.getLong("asyncEventCallId");
            String chatRoomName = message.getString("chatRoomName");
            wxId = message.getString("wxId");
            // 记录总的链接数和已完成的链接数用于最终完成情况判断
            count = message.getLong("count");
            // 唯一区分标识
            current = message.getLong("current");
            // 保存至redis,记录当前完成个数
            currentCount = (Long) redisTemplate.opsForValue().get("count::currentCount" + wxId + current);
            // 初次进来即为一
            if (currentCount == null) {
                currentCount = 1L;
            }
            log.info("群聊进度：总群数：{}，当前执行个数：{}", count, currentCount);
            weixinAsyncEventCall = weixinAsyncEventCallService.getById(asyncEventCallId);
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
                log.info("中间提前结束流程信息:{}", message);
                // 更新话术详情
                if (weixinTemplateSendDetail != null) {
                    weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult(weixinAsyncEventCall.getResult()));
                }
                return true;
            }
            JSONObject templateIds = message.getJSONObject("templateIds");
            // 验证小号是否存在群中，通过主号获取群成员列表方式
            JSONObject jsonObject = JSONObject.of("ChatRoomWxIdList", Lists.newArrayList(chatRoomName));
            MultiValueMap<String, String> queryBase = new LinkedMultiValueMap<>();
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            // 获取对应账号信息
            List<String> wxIds = message.getList("wxIds", String.class);
            List<String> keys = Lists.newArrayList();
            // 二次校验在线情况并构造对应的keys
            log.info("2、校验微信号在线情况");
            for (String wxId0 : wxIds) {
                WeixinBaseInfo weixinBaseInfo = weixinBaseInfoService.getById(wxId0);
                // 校验其是否依然在线
                if(weixinBaseInfo != null && StrUtil.equals("1", weixinBaseInfo.getState())){
                    keys.add(weixinBaseInfo.getKey());
                }else {
                    log.info("该微信不在线微信信息:{}", weixinBaseInfo);
                }
            }
            long countVo = 0;
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                queryBase.clear();
                queryBase.add("key", key);
                JSONObject chatroomInfo = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(jsonObject, queryBase);
                if (ResConstant.CODE_SUCCESS.equals(chatroomInfo.getInteger(ResConstant.CODE))) {
                    log.info("群详情返回数据：{}", chatroomInfo);
                    // 获取群头像信息
                    JSONObject contact = chatroomInfo.getJSONObject(ResConstant.DATA).getJSONArray("contactList").getJSONObject(0);
                    if(contact == null){
                        log.info("3.1、该账号key:{}已不在这个群内", key);
                        continue;
                    }
                    // String nickName = contact.getJSONObject("nickName").getString("str");
                    // String smallHeadImgUrl = contact.getString("smallHeadImgUrl");
                    // weixinTemplateSendDetail.setChatRoomName(new String(Base64.getEncoder().encode(nickName.getBytes(StandardCharsets.UTF_8)))).setHeadImgUrl(smallHeadImgUrl);
                    JSONArray newChatroomData = contact.getJSONObject("newChatroomData").getJSONArray("chatroom_member_list");
                    // 先获取对应的子账号列表
                    countVo = newChatroomData.stream().filter(o -> {
                        Map map = (Map) o;
                        JSONObject jsonObject1 = new JSONObject(map);
                        return wxIds.contains(jsonObject1.getString("user_name"));
                    }).count();
                }else {

                }
                if (countVo > 0) break;
            }
            // 校验这两个子账号是否还在群聊中
            log.info("3、校验账号时候还在群内");
            if (countVo == 0) {
                log.info("该群不包含子账户信息或者账号状态异常：{}", chatRoomName);
                weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("该群不包含子账户信息或者账号状态异常"));
                return true;
            }
            log.info("4、开始获取群发配置信息");
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
            log.info("4.1、群发群间模板隔配置时间min:{},max:{}", min, max);
            if(max < min){
                log.info("4.2、配置信息异常");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("群消息发送配置最大值最小值有误").setRealTime(LocalDateTime.now()));
                return true;
            }
            String type = countVo == 1 ? "single" : "double";
            List<Long> types = templateIds.getList(type, Long.class);
            // 双人模板话术
            // 维护至redis中，标识当前微信执行到的模板次序
            Integer count0 = (Integer) redisTemplate.opsForValue().get("count::" + type + wxId);
            if (count0 == null) count0 = 0;
            // 校验该批次是否还是有些状态
            // 获取对应文件信息
            log.info("5、 选择，模板类型 wxId : {} 模板方式：{}", wxId, type);
            List<WeixinTemplateDetail> list = weixinTemplateDetailService.list(Wrappers.lambdaQuery(WeixinTemplateDetail.class).in(WeixinTemplateDetail::getTemplateId, types));
            // 对具体模板进行分组
            Map<Long, List<WeixinTemplateDetail>> WeixinTemplateDetailMap = list.stream().collect(Collectors.groupingBy(WeixinTemplateDetail::getTemplateId));
            Collection<List<WeixinTemplateDetail>> values = WeixinTemplateDetailMap.values();
            List<List<WeixinTemplateDetail>> lists = Lists.newArrayList(values);
            // step one 遍历模板列表
            List<WeixinTemplateDetail> weixinTemplateDetails = lists.get(count0 % lists.size());
            // 获取模板id和模板名称
            WeixinTemplate weixinTemplate = new WeixinTemplate();
            if (weixinTemplateDetails.size() > 0) {
                WeixinTemplateDetail weixinTemplateDetail1 = weixinTemplateDetails.get(0);
                weixinTemplate = weixinTemplateService.getById(weixinTemplateDetail1.getTemplateId());
                log.info("当前具体模板信息：{}", weixinTemplate);
            }
            log.info("当前count值：{}", count0);
            // 每隔两秒执行一次
            log.info("6、开始发送具体模板数据");
            for (int i = 0; i < weixinTemplateDetails.size(); i++) {
                log.info("执行到批次：{}", i);
                List<JSONObject> jsonObjectList = Lists.newArrayList();
                JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
                jsonObjectList.add(param);
                JSONObject paramVo = JSONObject.of("MsgItem", jsonObjectList);
                WeixinTemplateDetail weixinTemplateDetail = weixinTemplateDetails.get(i);
                // 构造模板参数
                String key = "A".equals(weixinTemplateDetail.getMsgRole()) ? keys.get(0) : keys.get(1);
                query.add("key", key);
                // 查询当前号码是还在否在群内,还是通过主账号查询
                if (i > 0) {
                    // 获取子号是否还在群内
                    queryBase.clear();
                    queryBase.add("key", key);
                    log.info("6.1 、发送前二次校验账号是否还在群内");
                    JSONObject chatroomMemberDetailVo = WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(jsonObject, queryBase);
                    if (!ResConstant.CODE_SUCCESS.equals(chatroomMemberDetailVo.getInteger(ResConstant.CODE))) {
                        log.error("账号二次校验时异常 key:{}, 结束本轮操作", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("发送前二次校验账号是否还在群内：" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("发送前二次校验账号是否还在群内").setRealTime(LocalDateTime.now()));
                        return true;
                    }
                }
                // 1默认为普通文字消息
                if ("1".equals(weixinTemplateDetail.getMsgType())) {
                    param.put("AtWxIDList", null);
                    param.put("MsgType", 1);
                    param.put("TextContent", weixinTemplateDetail.getMsgContent() /*+ ",执行时间："+DateUtil.formatDateTime(new Date())*/);
                    // 发送文字信息
                    JSONObject textMessage = WechatApiHelper.SEND_TEXT_MESSAGE.invoke(paramVo, query);
                    log.info("6.2、发送文本消息：当前时间：{}，发送内容：{}， 返回值：{}，", new Date(), weixinTemplateDetail.getMsgContent(), textMessage);
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
                    // 发送图片信息
                } else if("0".equals(weixinTemplateDetail.getMsgType())){
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
                        log.error("账号发送图片信息异常 key:{}, 终止整个流程", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("账号发送图片信息异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("账号发送图片信息异常key:" + key).setRealTime(LocalDateTime.now()));
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
                    // 组装链接方式发送信息
                }else if("2".equals(weixinTemplateDetail.getMsgType())){
                    // 获取对应的拼接的链接信息
                    String msgContent = weixinTemplateDetail.getMsgContent();
                    WeixinAppMessage weixinAppMessage = weixinAppMessageService.getById(msgContent);
                    // 组装对应的xml数据信息
                    Document document = XmlUtil.mapToXml(BeanUtil.beanToMap(weixinAppMessage/*.setThumburl(basePath + weixinAppMessage.getThumburl())*/, false, false), "appmsg");
                    String msg = XmlUtil.toStr(document);
                    paramVo = JSONObject.of("AppList", JSONArray.of(JSONObject.of("ToUserName", chatRoomName, "ContentXML", msg, "ContentType", 49)));
                    JSONObject sendAppMessage = WechatApiHelper.SEND_APP_MESSAGE.invoke(paramVo, query);
                    if (!ResConstant.CODE_SUCCESS.equals(sendAppMessage.getInteger(ResConstant.CODE))) {
                        // TODO 细化具体异常处理
                        log.error("账号链接信息异常 key:{}, 终止整个流程", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("账号发送链接信息异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("账号发送链接信息异常key:" + key).setRealTime(LocalDateTime.now()));
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
                } else if ("3".equals(weixinTemplateDetail.getMsgType())){
                    JSONObject videoParam = JSONObject.of("ToUserName", chatRoomName);
                    // 获取图片信息用于展示
                    try {
                        File file = new File(basePath + weixinTemplateDetail.getMsgContent());
                        videoParam.put("VideoData", FileUtil.readBytes(file));
                        videoParam.put("ThumbData", "data:image/jpg;base64,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");
                    }catch (Exception e){
                        paramVo.clear();
                        query.clear();
                        log.error("视频获取异常，跳过本次发送");
                        continue;
                    }
                    JSONObject videoMessage = WechatApiHelper.CDN_UPLOAD_VIDEO.invoke(videoParam, query);
                    String cdnVideoUrl = videoMessage.getJSONObject("Data").getString("FileID");
                    String fileAesKey = videoMessage.getJSONObject("Data").getString("FileAesKey");
                    Integer videoDataSize = videoMessage.getJSONObject("Data").getInteger("VideoDataSize");
                    Integer thumbDataSize = videoMessage.getJSONObject("Data").getInteger("ThumbDataSize");

                    List<JSONObject> videoJsonObjectList = Lists.newArrayList();
                    JSONObject videoParam1 = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
                    videoParam1.put("AesKey",fileAesKey);
                    videoParam1.put("CdnVideoUrl",cdnVideoUrl);
                    videoParam1.put("Length",videoDataSize);
                    videoParam1.put("CdnThumbLength",thumbDataSize);
                    videoParam1.put("PlayLength",4);
                    videoJsonObjectList.add(videoParam1);
                    JSONObject forwardVideoParam = JSONObject.of("ForwardVideoList", videoJsonObjectList);
                    JSONObject ForwardVideoMessage = WechatApiHelper.FORWARD_VIDEO_MESSAGE.invoke(forwardVideoParam, query);

                    if (!ResConstant.CODE_SUCCESS.equals(ForwardVideoMessage.getInteger(ResConstant.CODE))) {
                        log.error("账号发送视频信息异常 key:{}, 终止整个流程", key);
                        weixinTemplateSendDetailService.updateById(weixinTemplateSendDetail.setStatus("500").setResult("账号发送视频信息异常key" + key));
                        weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("账号发送视频信息异常key:" + key).setRealTime(LocalDateTime.now()));
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
            redisTemplate.opsForValue().set("count::" + type + wxId, ++count0);
            weixinTemplateSendDetailService.updateById(
                    weixinTemplateSendDetail
                            .setTemplateId(weixinTemplate.getTemplateId())
                            .setFinishTime(LocalDateTime.now())
                            .setStatus("200")
                            .setResult("群发完成"));
            return true;
        }catch (Exception e){
            log.info("该群出现未知异常信息");
            e.printStackTrace();
            return true;
        }finally {
            log.info("群聊更新当前状态批次具体状态currentCount：{}count :{}", currentCount, count);
            if (count.equals(currentCount) && StrUtil.equals("99", weixinAsyncEventCall.getResultCode().toString())) {
                log.info("更新链接进群完成标识,并更新真实完成时间，重置模板批次");
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(200).setRealTime(LocalDateTime.now()));
                redisTemplate.opsForValue().set("count::double" + wxId, 0);
                redisTemplate.opsForValue().set("count::single" + wxId, 0);
                redisTemplate.delete("count::currentCount" + wxId + current);
            }else {
                redisTemplate.opsForValue().set("count::currentCount" + wxId + current, ++currentCount);
            }
        }
    }

}
