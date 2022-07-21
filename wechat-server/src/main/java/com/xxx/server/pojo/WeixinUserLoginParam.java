package com.xxx.server.pojo;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:UserLogin Description:用户登录实体
 * @author: lc
 * @date 2022/7/21 19:51
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "UserLogin对象",description = "")
public class WeixinUserLoginParam {
    @ApiModelProperty(value = "用户名",required = true)
    private String userName;
    @ApiModelProperty(value = "密码",required = true)
    private String passWord;
}
