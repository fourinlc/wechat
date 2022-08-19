package com.xxx.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class GroupChatNewParam {
    private List<String> chatRoomNames;
    private String wxIdA;
    private String wxIdB;
    private List<Long> templateIds;
    private Date fixedTime;
}
