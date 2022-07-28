package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.RepeatValid;
import com.xxx.server.annotation.valid.UpdateValid;
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
 * @since 2022-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_tempalate")
@ApiModel(value="WeixinTempalate对象")
@RepeatValid(groups = {AddValid.class, UpdateValid.class}, message = "该模板名已存在",fieldName = "templateName")
public class WeixinTempalate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "模板id")
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    @ApiModelProperty(value = "模板名称【是否冗余】")
    @TableField("template_name")
    private String templateName;

    @ApiModelProperty(value = "模板内容")
    @TableField("template_content")
    private String templateContent;

    @ApiModelProperty(value = "模板类型")
    @TableField("template_type")
    private String templateType;

    @ApiModelProperty(value = "消息类型")
    @TableField("msg_type")
    private String msgType;

    @ApiModelProperty(value = "单条模板状态")
    @TableField("template_status")
    private Integer templateStatus;

    @ApiModelProperty(value = "模板顺序")
    @TableField("template_order")
    private Integer templateOrder;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_user")
    private Long createUser;

    @TableField("update_user")
    private Long updateUser;


}
