package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinTemplateSendDetail;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-08-16
 */
public interface IWeixinTemplateSendDetailService extends IService<WeixinTemplateSendDetail> {
    /**获取好友群列表*/
    List<WeixinTemplateSendDetail> queryList(String wxId, boolean refresh);

    List<WeixinTemplateSendDetail> queryList(Long asyncEventCallId);

}
