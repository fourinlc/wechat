package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinFile;

/**
 * <p>
 * 文件信息 服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-24
 */
public interface IWeixinFileService extends IService<WeixinFile> {

    RespBean uploadFile(byte[] datas, String filePath, String filename);

    RespBean downFile(Long fileId);

}
