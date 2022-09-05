package com.xxx.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class GroupChatNewParam {
    private List<WeixinContactDetailedInfo> weixinContactDetailedInfos;
    private List<String> wxIds;
    private List<Long> templateIds;
    private Date fixedTime;
}
