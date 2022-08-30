package com.xxx.server.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:DeviceInfo Description:
 * @author: lc
 * @date 2022/8/30 15:20
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "DeviceInfo对象",description = "")
public class DeviceInfo {
    @ApiModelProperty(value = "Language",required = false)
    private String language;
}
