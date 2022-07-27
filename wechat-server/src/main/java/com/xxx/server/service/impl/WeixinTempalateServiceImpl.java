package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinFileMapper;
import com.xxx.server.mapper.WeixinTempalateMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinFile;
import com.xxx.server.pojo.WeixinTempalate;
import com.xxx.server.service.IWeixinFileService;
import com.xxx.server.service.IWeixinTempalateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
public class WeixinTempalateServiceImpl extends ServiceImpl<WeixinTempalateMapper, WeixinTempalate> implements IWeixinTempalateService {

    //TODO 设置间隔群聊时间，群群间隔时间
    private String time;

    private IWeixinFileService weixinFileService;

    // AB话术相互群聊
    @Override
    public void chatHandler(List<String> chatRoomNames, String keyA, String keyB, String templateName, List<Long> fileIds) {
        // 获取待加入的图片列表
        List<JSONObject> weixinFiles = weixinFileService.downFile(fileIds);
        Assert.isTrue(weixinFiles.size() > 0, "图片模板有误");
        // 获取对应文件信息
        for (int i = 0; i < chatRoomNames.size(); i++) {
            String chatRoomName = chatRoomNames.get(i);
            // step one 遍历模板列表
            List<WeixinTempalate> weixinTempalates = baseMapper
                    .selectList(Wrappers.<WeixinTempalate>lambdaQuery()
                            .eq(WeixinTempalate::getTemplateName, templateName)
                            .orderByAsc(WeixinTempalate::getTemplateOrder));
            Assert.isTrue(!weixinTempalates.isEmpty(), "模板信息有误");
            JSONObject param = JSONObject.of("ToUserName", chatRoomName, "Delay", true);
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
            for (WeixinTempalate weixinTempalate : weixinTempalates) {
                // 构造模板参数
                query.add("key", "A".equals(weixinTempalate.getTemplateType()) ? keyA : keyB);
                // 1默认为普通文字消息
                if ("1".equals(weixinTempalate.getMsgType())) {
                    param.put("AtWxIDList", null);
                    param.put("MsgType", 1);
                    param.put("TextContent", weixinTempalate.getTemplateContent());
                    // 发送文字信息
                    // step two 校验AB账号登录状态,发送消息的时候是否会自动校验
                    WechatApiHelper.SEND_TEXT_MESSAGE.invoke(param, query);
                } else {
                    param.put("TextContent", "");
                    param.put("ImageContent", weixinTempalate.getTemplateContent());
                    WechatApiHelper.SEND_IMAGE_MESSAGE.invoke(param, query);
                }
                // 清空param、query参数
                param.clear();
                query.clear();
            }
            // 最后添加自定义二维码信息数据
            JSONObject weixinFile = weixinFiles.get(i / weixinFiles.size());
            // 默认为A角色发送
            query.add("key", keyA);
            param.put("TextContent", "");
            param.put("ImageContent", weixinFile.getString("dataContext"));
            // 此时单个群操作完毕
            WechatApiHelper.SEND_IMAGE_MESSAGE.invoke(param, query);
        }
    }
}
