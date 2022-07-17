package com.xxx.server.enums;

import com.xxx.server.util.RestClient;
import com.xxx.server.util.SpringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

/**
 * 微信枚举信息
 */
public enum WechatApiHelper {


    GET_LOGIN_QRCODE_NEW("获取二维码", "/v1/login/GetLoginQrCodeNew", HttpMethod.POST),
    WAKEUP_LOGIN("唤醒登录", "/v1/login/WakeUpLogin", HttpMethod.POST),
    GET_CONTACT_LIST("分页获取联系人", "/v1/user/GetContactList", HttpMethod.POST),
    GET_PROFILE("获取个人信息", "/v1/user/GetProfile", HttpMethod.GET);

    private String desc;

    private String code;

    private HttpMethod httpMethod;

    private static final RestClient restclient = SpringUtils.getBean(RestClient.class);

    // 通用调用参数处理
    public Object invoke(Object param, MultiValueMap multiValueMap){
        switch (getHttpMethod()){
            case POST:
                return restclient.postJson(getCode(), param, multiValueMap);
            case GET:
                return restclient.getForm(getCode(), param, multiValueMap);
            default:
                return null;
        }
    }

    WechatApiHelper(String desc, String code, HttpMethod httpMethod){
        this.code = code;
        this.desc = desc;
        this.httpMethod = httpMethod;
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
