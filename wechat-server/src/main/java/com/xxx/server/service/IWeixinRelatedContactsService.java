package com.xxx.server.service;

import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-08-09
 */
public interface IWeixinRelatedContactsService extends IService<WeixinRelatedContacts> {

    RespBean relatedFriends(String wxId, List<String> relatedWxIds);

    RespBean getRelatedFriends(String wxId);

    RespBean cancelRelatedFriends(String wxId, List<String> relatedWxIds);
}
