package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinTempalate;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinTempalateService extends IService<WeixinTempalate> {

    void chatHandler(List<String> chatRoomNames, String keyA, String keyB, String templateName, List<Long> fileIds);
}
