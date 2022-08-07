package com.xxx.server.enums;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.util.RestClient;
import com.xxx.server.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

/**
 * 微信枚举信息
 */
@Slf4j
public enum WechatApiHelper {

    GET_LOGIN_QRCODE_NEW("获取二维码", "/v1/login/GetLoginQrCodeNew", HttpMethod.POST),
    CHECK_LOGIN_STATUS("登录检测", "/v1/login/CheckLoginStatus", HttpMethod.GET),
    WAKEUP_LOGIN("唤醒登录", "/v1/login/WakeUpLogin", HttpMethod.POST),
    LOG_OUT("退出登录","/v1/user/LogOut",HttpMethod.GET),
    GET_CONTACT_LIST("分页获取联系人", "/v1/user/GetContactList", HttpMethod.POST),
    GET_CONTACT_DETAILS_LIST("批量获取好友详情","/v1/user/GetContactDetailsList", HttpMethod.POST),
    GET_PROFILE("获取个人信息", "/v1/user/GetProfile", HttpMethod.GET),
    SEND_TEXT_MESSAGE("发送文字消息", "/v1/message/SendTextMessage", HttpMethod.POST){
        @Override
        public Object invoke(Object param, MultiValueMap<String, String> multiValueMap) {
            // 模拟处理群发消息文字版本
            log.info("开始发送文字消息：param ：{}，multiValueMap ：{}", param , multiValueMap);
            return JSONObject.of("code", 200);
        }
    },
    SEND_IMAGE_MESSAGE("发送图片", "/v1/message/SendImageMessage", HttpMethod.POST){
        @Override
        public Object invoke(Object param, MultiValueMap<String, String> multiValueMap) {
            // 模拟处理群发消息文字版本
            log.info("开始发送图片消息：param ：{}，multiValueMap ：{}", param , multiValueMap);
            return JSONObject.of("code", 200);
        }
    },
    GET_REDIS_SYNC_MSG("长链接订阅同步消息", "/v1/user/GetRedisSyncMsg", HttpMethod.POST),
    NEW_SYNC_HISTORY_MESSAGE("短链接同步消息", "/v1/user/NewSyncHistoryMessage", HttpMethod.POST),
    SCAN_INTO_URL_GROUP("同意进群", "/v1/group/ScanIntoUrlGroup", HttpMethod.POST){
        @Override
        public Object invoke(Object param, MultiValueMap<String, String> multiValueMap) {
            // 模拟处理群发消息文字版本
            log.info("模拟进群操作：param ：{}，multiValueMap ：{}", param , multiValueMap);
            JSONObject jsonObject = JSONObject.of("chatroomUrl", "1111@chaoom");
            return JSONObject.of("Code", 200, "Data", jsonObject);
        }
    },
    ADD_CHATROOM_MEMBERS("邀请进群", "/v1/group/AddChatRoomMembers", HttpMethod.POST){
        @Override
        public Object invoke(Object param, MultiValueMap<String, String> multiValueMap) {
            // 模拟处理群发消息文字版本
            log.info("邀请进群：param ：{}，multiValueMap ：{}", param , multiValueMap);

            return JSONObject.of("Code", 200);
        }
    },
    MOVETO_CONTRACT("保存群聊", "/v1/group/MoveToContract", HttpMethod.POST){
        @Override
        public Object invoke(Object param, MultiValueMap<String, String> multiValueMap) {
            // 模拟处理群发消息文字版本
            log.info("保存群聊：param ：{}，multiValueMap ：{}", param , multiValueMap);
            return JSONObject.of("Code", 200);
        }
    };

    private String desc;

    private String code;

    private HttpMethod httpMethod;

    private static final RestClient restclient = SpringUtils.getBean(RestClient.class);

    // 通用调用参数处理
    public Object invoke(Object param, MultiValueMap<String,String> multiValueMap){
        log.info("调用wechat统一入参信息：接口名称描述：{} param:{}, query:{}", getDesc(), param, multiValueMap);
        Object o;
        switch (getHttpMethod()){
            case POST:
                o = restclient.postJson(getCode(), param, multiValueMap);
                break;
            case GET:
                // 是否再区别更细分，依据Content-Type,当然可以直接子类重写相关
                o = restclient.getForm(getCode(), param, multiValueMap);
                break;
            default:
                return null;
        }
        log.info("调用wechat统一返回结果：{}", o);
        return o;
    }

    WechatApiHelper(String desc, String code, HttpMethod httpMethod){
        this.code = code;
        this.desc = desc;
        this.httpMethod = httpMethod;
    }

    public static WechatApiHelper getWechatApiHelper(String code){
        for (WechatApiHelper wechatApiHelper : WechatApiHelper.values()) {
            if (StrUtil.equals(code, wechatApiHelper.getCode())){
                return wechatApiHelper;
            }
        }
        return null;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }
}
