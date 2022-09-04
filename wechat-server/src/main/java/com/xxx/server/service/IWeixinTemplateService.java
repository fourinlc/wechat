package com.xxx.server.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.UpdateValid;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.pojo.WeixinTemplateDetail;
import com.xxx.server.pojo.WeixinTemplateParam;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@Validated
public interface IWeixinTemplateService extends IService<WeixinTemplate> {

    @Validated(AddValid.class)
    boolean add(@Valid WeixinTemplate weixinTemplate,List<WeixinTemplateDetail> weixinTemplateDetails);

    @Validated(UpdateValid.class)
    boolean update(@Valid WeixinTemplate weixinTemplate, List<WeixinTemplateDetail> weixinTemplateDetails);

    List<WeixinTemplateParam> queryList(WeixinTemplate weixinTemplate);

    boolean deleteByName(String templateName);

    JSONObject groupChat(List<String> chatRoomNames, String wxId, List<Long> templateIds, Date fixedTime);

    JSONObject groupChatNew(List<String> chatRoomNames, List<String> wxIds, List<Long> templateIds, Date fixedTime);
}
