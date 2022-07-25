package com.xxx.server.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:WeixinUserRegisterParam Description:
 * @author: lc
 * @date 2022/7/25 9:37
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "UserLogin对象",description = "")
public class WeixinUserRegisterParam {

    @ApiModelProperty(value = "用户名",required = true)
    private String userName;
    @ApiModelProperty(value = "密码",required = true)
    private String passWord;
    @ApiModelProperty(value = "用户类型",required = true)
    private String userType;
}
