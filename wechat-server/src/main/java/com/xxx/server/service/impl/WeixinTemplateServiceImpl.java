package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import com.xxx.server.mapper.WeixinTemplateMapper;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.pojo.WeixinTemplateDetail;
import com.xxx.server.service.IWeixinFileService;
import com.xxx.server.service.IWeixinTemplateDetailService;
import com.xxx.server.service.IWeixinTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
public class WeixinTemplateServiceImpl extends ServiceImpl<WeixinTemplateMapper, WeixinTemplate> implements IWeixinTemplateService {

    @Resource
    private IWeixinFileService weixinFileService;
    @Resource
    private DelayMqProducer delayMqProducer;

    @Resource
    private IWeixinTemplateDetailService weixinTemplateDetailService;

    @Value("${spring.rocketmq.consumer-topic}")
    private String consumerTopic;

    // AB话术相互群聊
    // @Override
    /*public void chatHandler(List<String> chatRoomNames, String keyA, String keyB, String templateName, List<Long> fileIds) throws InterruptedException {
        // 获取对应文件信息
        for (int i = 0; i < chatRoomNames.size(); i++) {
            String chatRoomName = chatRoomNames.get(i);
            // step one 遍历模板列表
            List<WeixinTemplate> weixinTempalates = baseMapper
                    .selectList(Wrappers.<WeixinTemplate>lambdaQuery()
                            .eq(WeixinTemplate::getTemplateName, templateName)
                            .orderByAsc(WeixinTemplate::getTemplateOrder));
            Assert.isTrue(!weixinTempalates.isEmpty(), "模板信息有误");
            // 发送延时消息至rocketmq
            JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            Date delay = new Date();
            // 每隔两秒执行一次
            String code = WechatApiHelper.SEND_TEXT_MESSAGE.getCode();
            for (WeixinTemplate weixinTempalate : weixinTempalates) {
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
        }
    }*/

     @Transactional
    public boolean add(WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails){
        if (save(weixinTemplate)) {
            // 填充id
            for (WeixinTemplateDetail weixinTemplateDetail : weixinTemplateDetails) {
                weixinTemplateDetail.setTemplateId(weixinTemplate.getTemplateId());
            }
        }
        // 批量新增
        return weixinTemplateDetailService.saveBatch(weixinTemplateDetails);
    }

    @Transactional
    public boolean update( WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails){
        if (updateById(weixinTemplate)) {
            // 填充id,删除老数据
            weixinTemplateDetailService.removeByMap(JSONObject.of("template_id", weixinTemplate.getTemplateId()));
            for (WeixinTemplateDetail weixinTemplateDetail : weixinTemplateDetails) {
                weixinTemplateDetail.setTemplateId(weixinTemplate.getTemplateId());
            }
        }
        // 批量新增
        return weixinTemplateDetailService.saveBatch(weixinTemplateDetails);
    }


}
