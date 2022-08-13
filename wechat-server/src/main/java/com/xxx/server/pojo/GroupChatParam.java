package com.xxx.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class GroupChatParam {
    private List<String> chatRoomNames;
    private String wxId;
    private List<Long> templateIds;
}
