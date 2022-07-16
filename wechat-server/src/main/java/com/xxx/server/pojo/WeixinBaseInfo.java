package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
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
 * @since 2022-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_base_info")
@ApiModel(value="WeixinBaseInfo对象", description="")
public class WeixinBaseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "微信id")
    @TableId("weixin_id")
    private String weixinId;

    @ApiModelProperty(value = "微信昵称")
    @TableField("weixin_nick")
    private String weixinNick;

    @ApiModelProperty(value = "登录状态")
    @TableField("login_status")
    private String loginStatus;

    @ApiModelProperty(value = "群个数")
    @TableField("chat_room_count")
    private Integer chatRoomCount;

    @ApiModelProperty(value = "登录返回的key")
    private String key;

    @ApiModelProperty(value = "登录返回的uuid")
    private String uuid;

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
