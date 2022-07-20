package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinTempalate;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinTempalateService extends IService<WeixinTempalate> {

    void chatHandler(String chatRoomName, String keyA, String keyB, String templateName);

}
