package com.xxx.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class GroupSendParam {

    private List<WeixinContactDetailedInfo> weixinContactDetailedInfos;

    private String masterWxId;

    private List<String> slaveWxIds;

    private boolean flag;

}
