package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.RepeatValid;
import com.xxx.server.annotation.valid.UpdateValid;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author lc
 * @since 2022-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_tempalate")
@ApiModel(value="WeixinTemplate")
@RepeatValid(groups = {AddValid.class, UpdateValid.class}, message = "该模板名已存在",fieldName = "templateName")
public class WeixinTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "模板id")
    @TableId(value = "template_id", type = IdType.AUTO)
    @NotNull(groups = UpdateValid.class, message = "更新时模板id不能为空")
    private Long templateId;

    @ApiModelProperty(value = "模板名称")
    @TableField("template_name")
    @NotEmpty(groups = AddValid.class, message = "新增模板名称不能为空")
    private String templateName;

    @ApiModelProperty(value = "模板内容")
    @TableField("template_content")
    @NotEmpty(groups = AddValid.class, message = "新增模板内容不能为空")
    private String templateContent;

    @ApiModelProperty(value = "模板类型 single : double")
    @TableField("template_type")
    private String templateType;

    @ApiModelProperty(value = "消息类型")
    @TableField("msg_type")
    @NotEmpty(groups = AddValid.class, message = "消息类型内容不能为空")
    private String msgType;

    @ApiModelProperty(value = "模板角色类型 A : B")
    @TableField("template_role")
    @NotEmpty(groups = AddValid.class, message = "模板角色类型不能为空")
    private String templateRole;

    @ApiModelProperty(value = "单条模板状态")
    @TableField("template_status")
    private Integer templateStatus;

    @ApiModelProperty(value = "模板顺序")
    @TableField("template_order")
    @NotNull(groups = AddValid.class, message = "模板顺序不能为空")
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
