package com.xxx.server.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:DeviceLoginParam Description:
 * @author: lc
 * @date 2022/8/30 15:16
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "DeviceLogin对象",description = "")
public class DeviceLoginParam {
    @ApiModelProperty(value = "16/62数据",required = true)
    private String deviceId;
    @ApiModelProperty(value = "账号",required = true)
    private String userName;
    @ApiModelProperty(value = "密码",required = true)
    private String passWord;
    @ApiModelProperty(value = "代理(\"Proxy\": \"socks5://puser:8rrZPggTKU@120.79.162.3:1080\")",required = false)
    private String proxy;
    @ApiModelProperty(value = "DeviceInfo",required = false)
    private DeviceInfo deviceInfo;
}
