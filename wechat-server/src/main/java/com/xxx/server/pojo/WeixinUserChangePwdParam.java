package com.xxx.server.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:WeixinUserChangePwdParam Description:
 * @author: lc
 * @date 2022/8/4 17:47
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "changePassword对象",description = "")
public class WeixinUserChangePwdParam {
    @ApiModelProperty(value = "用户名",required = true)
    private String userName;
    @ApiModelProperty(value = "旧密码",required = true)
    private String oldPassWord;
    @ApiModelProperty(value = "新密码",required = true)
    private String newPassWord;
}
