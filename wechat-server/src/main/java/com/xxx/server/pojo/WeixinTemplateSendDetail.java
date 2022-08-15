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
 * @since 2022-08-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_template_send_detail")
@ApiModel(value="WeixinTemplateSendDetail对象", description="")
public class WeixinTemplateSendDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "template_send_detail_id", type = IdType.AUTO)
    private Long templateSendDetailId;

    @ApiModelProperty(value = "处理批次")
    @TableField("async_event_call_id")
    private Long asyncEventCallId;

    @ApiModelProperty(value = "模板id")
    @TableField("template_id")
    private Long templateId;

    @ApiModelProperty(value = "群id")
    @TableField("chat_room_id")
    private String chatRoomId;

    @ApiModelProperty(value = "群名")
    @TableField("chat_room_name")
    private String chatRoomName;

    @ApiModelProperty(value = "处理状态99：处理中，0，处理失败，1，处理成功")
    private String status;

    @ApiModelProperty(value = "完成时间")
    @TableField("finish_time")
    private LocalDateTime finishTime;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
