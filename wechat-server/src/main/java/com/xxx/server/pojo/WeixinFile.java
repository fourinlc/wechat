package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文件信息
 * </p>
 *
 * @author lc
 * @since 2022-07-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_file")
@ApiModel(value="WeixinFile对象", description="文件信息")
public class WeixinFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    @ApiModelProperty(value = "原始名称")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty(value = "文件相对路径")
    @TableField("file_path")
    private String filePath;

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
