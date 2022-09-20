package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xxx.server.annotation.dict.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

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
@ApiModel(value = "WeixinGroupLinkDetail对象")
public class WeixinGroupLinkDetail implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "link_id", type = IdType.AUTO)
    private Long linkId;

    @ApiModelProperty(value = "进群群链接,对应消息内容")
    private String content;

    @ApiModelProperty(value = "微信消息id")
    @TableField("msg_id")
    private Long msgId;

    @ApiModelProperty(value = "群链接状态0：未操作，1：进群成功，2:保存群聊，3：邀请子账号完成 4：处理完成 500：处理失败 99：处理中")
    @TableField("link_status")
    @Dict
    private String linkStatus;

    @ApiModelProperty(value = "重复标识")
    @TableField("repeat_status")
    private String repeatStatus;

    @ApiModelProperty(value = "失效标识")
    @TableField("invalid_status")
    private String invalidStatus;

    @ApiModelProperty(value = "验证群标识")
    @TableField("verify_status")
    private String verifyStatus;

    @ApiModelProperty(value = "企业微信群标识")
    @TableField("company_status")
    private String companyStatus;

    @ApiModelProperty(value = "邀请人微信名称")
    @TableField("from_user_name")
    private String fromUserName;

    @ApiModelProperty(value = "邀请人微信id")
    @TableField("from_user_wxId")
    private String fromUserWxId;

    @ApiModelProperty(value = "邀请备注")
    private String remark;

    @ApiModelProperty(value = "消息创建时间")
    @TableField("create_time")
    private Long createTime;

    @ApiModelProperty(value = "被邀请人微信名称")
    @TableField("to_user_name")
    private String toUserName;

    @ApiModelProperty(value = "被邀请人微信id")
    @TableField("to_user_wxId")
    private String toUserWxId;

    @TableField(exist = false)
    private Integer msgType;

    @ApiModelProperty(value = "邀请时间")
    @TableField("invitation_time")
    private String invitationTime;

    @TableField("chatroom_name")
    private String chatroomName;

    @TableField("`key`")
    private String key;

    @ApiModelProperty(value = "群id的概念，用于区分是否重复群")
    @TableField("thumb_url")
    private String thumbUrl;

    @TableField("result")
    private String result;

    @TableField("group_send_detail_id")
    private Long groupSendDetailId;

    @TableField("async_event_call_id")
    private Long asyncEventCallId;

    @TableField("update_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date updateTime;


    @Override
    public WeixinGroupLinkDetail clone() {
        try {
            return (WeixinGroupLinkDetail) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
