package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
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
 * @since 2022-08-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_group_send_detail")
@ApiModel(value="WeixinGroupSendDetail对象", description="")
public class WeixinGroupSendDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "group_send_detail_id", type = IdType.AUTO)
    private Long groupSendDetailId;

    @TableField("async_event_call_id")
    private Long asyncEventCallId;

    @TableField("chat_room_id")
    private String chatRoomId;

    @TableField("chat_room_name")
    private String chatRoomName;

    private String status;

    private String result;

    @ApiModelProperty(value = "邀请微信id")
    @TableField("master_wx_id")
    private String masterWxId;

    /*@ApiModelProperty(value = "被邀请微信id")
    @TableField("slave_wx_id")
    private String slaveWxId;*/

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("create_time")
    private LocalDate createTime;

    @TableField("small_headImg_url")
    private String smallHeadImgUrl;


}
