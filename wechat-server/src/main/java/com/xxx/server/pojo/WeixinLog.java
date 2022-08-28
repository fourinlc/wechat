package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author lc
 * @since 2022-08-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_log")
@ApiModel(value="WeixinLog对象", description="")
public class WeixinLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "weixin_log_id", type = IdType.AUTO)
    private Long weixinLogId;

    @ApiModelProperty(value = "接口名称")
    @TableField("method_name")
    private String methodName;

    @ApiModelProperty(value = "参数1")
    @TableField("method_param")
    private String methodParam;

    @ApiModelProperty(value = "链接参数，常见为key")
    @TableField("method_query")
    private String methodQuery;

    @ApiModelProperty(value = "返回详情")
    private String result;

    @ApiModelProperty(value = "状态码信息")
    private String status;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
