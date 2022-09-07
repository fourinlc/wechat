package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * @since 2022-08-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_template_detail")
@ApiModel(value="WeixinTemplateDetail对象", description="")
public class WeixinTemplateDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "template_detail_id", type = IdType.AUTO)
    private Long templateDetailId;

    @ApiModelProperty(value = "模板id")
    @TableField("template_id")
    private Long templateId;

    @ApiModelProperty(value = "消息内容")
    @TableField("msg_type")
    private String msgType;

    @ApiModelProperty(value = "消息内容")
    @TableField("msg_content")
    private String msgContent;

    @ApiModelProperty(value = "消息顺序")
    @TableField("msg_order")
    private Integer msgOrder;

    @ApiModelProperty(value = "消息角色")
    @TableField("msg_role")
    private String msgRole;

}
