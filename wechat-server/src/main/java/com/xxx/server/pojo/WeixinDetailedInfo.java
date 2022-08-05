package com.xxx.server.pojo;

import io.swagger.annotations.ApiModel;
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
@ApiModel(value = "weixinDetailedInfo",description = "")
public class WeixinDetailedInfo {
    @ApiModelProperty(value = "微信id")
    String wxId;
    @ApiModelProperty(value = "用户名")
    String userName;
    @ApiModelProperty(value = "性别")
    String sex;
    @ApiModelProperty(value = "签名")
    String signature;


}
