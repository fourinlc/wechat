package com.xxx.server.pojo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:WeixinDetailedInfo Description:
 * @author: lc
 * @date 2022/8/5 9:27
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeixinContactDetailedInfo {
    @ApiModelProperty(value = "微信id")
    String wxId;
    @ApiModelProperty(value = "用户名")
    String userName;
    @ApiModelProperty(value = "性别")
    String sex;
    @ApiModelProperty(value = "签名")
    String signature;
    @ApiModelProperty(value = "拥有者")
    String chatRoomOwner;
    @ApiModelProperty(value = "bigHeadImgUrl")
    String bigHeadImgUrl;
    @ApiModelProperty(value = "smallHeadImgUrl")
    String smallHeadImgUrl;
    @ApiModelProperty(value = "related")
    String related;
    @ApiModelProperty(value = "chatroomAccessType")
    String chatroomAccessType;
}
