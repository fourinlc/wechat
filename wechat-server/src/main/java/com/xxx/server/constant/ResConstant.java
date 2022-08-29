package com.xxx.server.constant;

/**
 * 微信返回参数实列
 */
public interface ResConstant {

    String CODE = "Code";

    String DATA = "Data";

    /**成功CODE*/
    Integer CODE_SUCCESS = 200;

    String PATTERN = "[0-9]+@chatroom";

    /**群邀请类型*/
    String ASYNC_EVENT_SCAN_INTO_URL_GROUP = "scanIntoUrlGroup";

    /**群聊类型*/
    String ASYNC_EVENT_GROUP_CHAT = "groupChat";

    /**群聊类型*/
    String ASYNC_EVENT_GROUP_SEND = "groupSend";


}
