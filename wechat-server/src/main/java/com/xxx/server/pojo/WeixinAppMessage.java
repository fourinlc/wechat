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
import springfox.documentation.annotations.ApiIgnore;

/**
 * <p>
 * 
 * </p>
 *
 * @author lc
 * @since 2022-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_app_message")
@ApiModel(value="WeixinAppMessage对象", description="")
public class WeixinAppMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "app_message_id", type = IdType.AUTO)
    private Long appMessageId;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "描述")
    private String des;

    @ApiModelProperty(hidden = true)
    private String action;

    @ApiModelProperty(hidden = true)
    private String type;

    @TableField("show_type")
    @ApiModelProperty(hidden = true)
    private String showType;

    @TableField("sound_type")
    @ApiModelProperty(hidden = true)
    private String soundType;

    @ApiModelProperty("链接跳转地址")
    private String url;

    @ApiModelProperty("图片展示")
    private String thumburl;


}
