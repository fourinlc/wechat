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
 * @since 2022-08-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_async_event_call")
@ApiModel(value="WexxinAsyncEventCall对象", description="")
public class WeixinAsyncEventCall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "async_event_call_id", type = IdType.AUTO )
    private Long asyncEventCallId;

    @ApiModelProperty(value = "微信id")
    private String wxId;

    @ApiModelProperty(value = "事件类型：0， 进群 ，1 ：话术模板")
    @TableField("event_type")
    private String eventType;

    @ApiModelProperty(value = "业务id，批次号")
    @TableField("business_id")
    private String businessId;

    @ApiModelProperty(value = "状态码")
    @TableField("result_code")
    private Integer resultCode;

    @ApiModelProperty(value = "预计完成时间")
    @TableField("plan_time")
    private LocalDateTime planTime;

    @ApiModelProperty(value = "实际完成时间")
    @TableField("real_time")
    private LocalDateTime realTime;

    @TableField("plan_start_time")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "结果描述")
    private String result;


}
