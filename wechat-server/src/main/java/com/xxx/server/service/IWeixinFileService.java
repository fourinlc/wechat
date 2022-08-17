package com.xxx.server.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinFile;

import java.util.List;

/**
 * <p>
 * 文件信息 服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-24
 */
public interface IWeixinFileService extends IService<WeixinFile> {

    /**
     * 上传文件至服务器
     * @param datas 文件流
     * @param filePath 文件相对路径
     * @param filename 文件名字
     * @return 返回文件相对路径
     */
    String uploadFile(byte[] datas, String filePath, String filename);

    /**
     * 获取单个文件流
     * @param fileId
     * @return
     */
    JSONObject downFile(Long fileId);

    /**
     * 批量获取文件流
     * @param fileIds 文件ids
     * @return
     */
    List<JSONObject> downFile(List<Long> fileIds);

}
