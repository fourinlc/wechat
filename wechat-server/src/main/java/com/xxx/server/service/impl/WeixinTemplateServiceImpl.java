package com.xxx.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinTemplateMapper;
import com.xxx.server.pojo.WeixinTempalate;
import com.xxx.server.service.IWeixinFileService;
import com.xxx.server.service.IWeixinTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 微信AB话术类
 * </p>
 *
 * @author xxx
 * @since 2022-07-16
 */
@Service
//@AllArgsConstructor
@Slf4j
public class WeixinTemplateServiceImpl extends ServiceImpl<WeixinTemplateMapper, WeixinTempalate> implements IWeixinTemplateService {

    @Resource
    private IWeixinFileService weixinFileService;
    @Resource
    private DelayMqProducer delayMqProducer;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    /*@Value("${spring.rocketmq.tags.qun}")
    private String consumerQunTag;*/

    // AB话术相互群聊
    @Override
    public void chatHandler(List<String> chatRoomNames, String keyA, String keyB, String templateName, List<Long> fileIds) throws InterruptedException {
        // 获取待加入的图片列表
        /*List<JSONObject> weixinFiles = weixinFileService.downFile(fileIds);
        Assert.isTrue(weixinFiles.size() > 0, "图片模板有误");*/
        // 获取对应文件信息
        for (int i = 0; i < chatRoomNames.size(); i++) {
            String chatRoomName = chatRoomNames.get(i);
            // step one 遍历模板列表
            List<WeixinTempalate> weixinTempalates = baseMapper
                    .selectList(Wrappers.<WeixinTempalate>lambdaQuery()
                            .eq(WeixinTempalate::getTemplateName, templateName)
                            .orderByAsc(WeixinTempalate::getTemplateOrder));
            Assert.isTrue(!weixinTempalates.isEmpty(), "模板信息有误");
            // 发送延时消息至rocketmq
            JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            Date delay = new Date();
            // 每隔两秒执行一次
            String code = WechatApiHelper.SEND_TEXT_MESSAGE.getCode();
            for (WeixinTempalate weixinTempalate : weixinTempalates) {
                // 构造模板参数
                query.add("key", "A".equals(weixinTempalate.getTemplateType()) ? keyA : keyB);
                // 1默认为普通文字消息
                if ("1".equals(weixinTempalate.getMsgType())) {
                    param.put("AtWxIDList", null);
                    param.put("MsgType", 1);
                    param.put("TextContent", weixinTempalate.getTemplateContent());
                    // 发送文字信息
                    // WechatApiHelper.SEND_TEXT_MESSAGE.invoke(param, query);
                } else {
                    param.put("TextContent", "");
                    param.put("ImageContent", weixinTempalate.getTemplateContent());
                    code = WechatApiHelper.SEND_IMAGE_MESSAGE.getCode();
                }
                // step two 校验AB账号登录状态,发送消息的时候是否会自动校验
                JSONObject msg = JSONObject.of("param", param, "query", query, "code", code);
                //TODO 是否有必要设置成异步消息,加快响应时间
                Message message = new Message(consumerTopic, "", JSON.toJSONBytes(msg));
                delay = DateUtils.addSeconds(delay, 2);
                delayMqProducer.sendDelay(message, delay);
                // 清空param、query参数
                param.clear();
                query.clear();
            }
            // 最后添加自定义二维码信息数据
            /*JSONObject weixinFile = weixinFiles.get(i / weixinFiles.size());
            // 默认为A角色发送
            query.add("key", keyA);
            param.put("TextContent", "");
            param.put("ImageContent", weixinFile.getString("dataContext"));
            code = WechatApiHelper.SEND_IMAGE_MESSAGE.getCode();
            JSONObject msg = JSONObject.of("param", param, "query", query, "code", code);
            Message message = MessageBuilder.of(JSON.toJSONBytes(msg)).topic("GuavaRocketConstants.PROXY_TOPIC").build();
            delay = DateUtils.addSeconds(delay, 2);
            delayMqProducer.sendDelay(message, delay);*/
            // 此时单个群操作完毕
            // WechatApiHelper.SEND_IMAGE_MESSAGE.invoke(param, query);
        }
    }

    @Override
    public List<WeixinTempalate> queryList(WeixinTempalate weixinTempalate) {
        return baseMapper.selectList(Wrappers.<WeixinTempalate>lambdaQuery()
                .like(StrUtil.isNotEmpty(weixinTempalate.getTemplateName()), WeixinTempalate::getTemplateName, weixinTempalate.getTemplateName())
                .orderByDesc(WeixinTempalate::getTemplateOrder));
    }
}
