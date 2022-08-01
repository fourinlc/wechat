package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinDictionary;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinDictionaryService extends IService<WeixinDictionary> {

    List<WeixinDictionary> query(WeixinDictionary weixinDictionary);
}
