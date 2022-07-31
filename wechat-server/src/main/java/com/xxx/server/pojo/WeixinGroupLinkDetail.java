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
 * @since 2022-07-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_group_link_detail")
@ApiModel(value="WeixinGroupLinkDetail对象", description="")
public class WeixinGroupLinkDetail implements Serializable,Cloneable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "link_id", type = IdType.AUTO)
    private Long linkId;

    @ApiModelProperty(value = "进群群链接,对应消息内容")
    private String content;

    @ApiModelProperty(value = "微信消息id")
    @TableField("msg_id")
    private Long msgId;

    @ApiModelProperty(value = "群链接状态0：未操作，1：失效，2:频繁，3：企业微信")
    @TableField("link_status")
    private String linkStatus;

    @ApiModelProperty(value = "邀请人微信id")
    @TableField("from_user_name")
    private String fromUserName;

    @ApiModelProperty(value = "群备注。")
    private String remark;

    @ApiModelProperty(value = "消息创建时间")
    @TableField("create_time")
    private Long createTime;

    @ApiModelProperty(value = "被邀请人微信id")
    @TableField("to_user_name")
    private String toUserName;

    @TableField(exist = false)
    private Integer msgType;


    @Override
    public WeixinGroupLinkDetail clone() {
        try {
            return (WeixinGroupLinkDetail) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
