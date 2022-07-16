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
 * @since 2022-07-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_group_link_detail")
@ApiModel(value="WeixinGroupLinkDetail对象", description="")
public class WeixinGroupLinkDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "link_id", type = IdType.AUTO)
    private Long linkId;

    @ApiModelProperty(value = "进群群链接")
    @TableField("group_link")
    private String groupLink;

    @ApiModelProperty(value = "群链接状态0：未操作，1：失效，2:频繁，3：企业微信")
    @TableField("link_status")
    private String linkStatus;

    @ApiModelProperty(value = "微信id")
    @TableField("weixin_id")
    private Long weixinId;

    @ApiModelProperty(value = "群备注。")
    private String remark;

    @ApiModelProperty(value = "执行时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
