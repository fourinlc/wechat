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

import javax.validation.constraints.NotEmpty;

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
@TableName("t_weixin_template")
@ApiModel(value="WeixinTemplate对象", description="")
@RepeatValid(groups = {AddValid.class}, message = "模板不能重复", fieldName = "templateName")
@RepeatValid(groups = {UpdateValid.class}, message = "模板不能重复", fieldName = "templateName")
public class WeixinTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("模板id")
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    @ApiModelProperty("模板名称")
    @TableField("template_name")
    @NotEmpty(groups = AddValid.class, message = "模板名称不能为空")
    private String templateName;

    @ApiModelProperty("模板类型,单人 双人")
    @TableField("template_type")
    @NotEmpty(groups = AddValid.class, message = "模板类型不能为空")
    private String templateType;

    @ApiModelProperty("单条模板状态")
    @TableField("template_status")
    private Integer templateStatus;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_user")
    private Long createUser;

    @TableField("update_user")
    private Long updateUser;


}
