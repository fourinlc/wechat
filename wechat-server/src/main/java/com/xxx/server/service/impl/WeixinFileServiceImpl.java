package com.xxx.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.xxx.server.mapper.WeixinFileMapper;
import com.xxx.server.pojo.WeixinFile;
import com.xxx.server.service.IWeixinFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 文件信息 服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-07-24
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeixinFileServiceImpl extends ServiceImpl<WeixinFileMapper, WeixinFile> implements IWeixinFileService {

    @Value("${wechat.file.basePath}")
    private String basePath;

    private final WeixinFileMapper weixinFileMapper;

    // 文件上传至本地
    public String uploadFile(byte[] datas, String filePath, String filename) {
        filePath = filePath.replaceAll("/", "");
        // 暂时不支持直接放在根文件夹下
        Assert.isTrue(StrUtil.isNotEmpty(filePath), "暂时不支持根文件夹上传");
        // 默认非追加模式，即为覆盖方式,失败时统一异常捕获
        // 增加时间戳区分唯一性
        FileUtil.writeBytes(datas, basePath + filePath + "/" + System.currentTimeMillis() + "/" + filename);
        // 成功时记录文件信息
        WeixinFile weixinFile = new WeixinFile().setFileName(filename).setFilePath(filePath);
        Assert.isTrue(weixinFileMapper.insert(weixinFile) > 0, "文件上传失败");
        return "/" + filePath + "/" + System.currentTimeMillis() + "/" + filename;
    }

    // 文件本地下载
    public JSONObject downFile(Long fileId) {
        // 暂时默认设置为主键下载
        WeixinFile weixinFileVo = weixinFileMapper.selectById(fileId);
        Assert.notNull(weixinFileVo, "文件不存在");
        // 根据文件路径获取对应的文件流信息
        byte[] bytes = FileUtil.readBytes(basePath + weixinFileVo.getFilePath() + "/" + weixinFileVo.getFileName());
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(weixinFileVo));
        jsonObject.put("dataContext", bytes);
        return jsonObject;
    }

    public List<JSONObject> downFile(List<Long> fileIds) {
        List<JSONObject> jsonObjects = Lists.newArrayList();
        for (Long fileId : fileIds) {
            jsonObjects.add(downFile(fileId));
        }
        return jsonObjects;
    }
}
