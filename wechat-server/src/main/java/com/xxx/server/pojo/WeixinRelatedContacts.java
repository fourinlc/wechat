package com.xxx.server.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * @since 2022-08-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_weixin_related_contacts")
@ApiModel(value="WeixinRelatedContacts对象", description="")
public class WeixinRelatedContacts implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("wx_id")
    private String wxId;

    private String related1;

    private String related2;


}
