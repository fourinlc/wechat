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
@TableName("t_weixin_dictionary")
@ApiModel(value="WeixinDictionary对象", description="")
public class WeixinDictionary implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主键")
    @TableId(value = "dic_id", type = IdType.AUTO)
    private Long dicId;

    @ApiModelProperty(value = "组别")
    @TableField("dic_group")
    private String dicGroup;

    @ApiModelProperty(value = "code值")
    @TableField("dic_code")
    private String dicCode;

    @ApiModelProperty(value = "key值")
    @TableField("dic_key")
    private String dicKey;

    @ApiModelProperty(value = "value值")
    @TableField("dic_value")
    private String dicValue;

    @ApiModelProperty(value = "预留字段")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
