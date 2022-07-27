package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.service.IWeixinFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 * 文件信息 前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-07-24
 */
@RestController
@RequestMapping("/weixin-file")
@AllArgsConstructor
@Api(tags = "文件处理")
public class WeixinFileController {

    private IWeixinFileService weixinFileService;

    @ApiOperation("文件上传")
    @PostMapping("/uploadFile")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath", dataType = "string", value = "文件存放相对路径", paramType = "query",example = "111"),
    })
    public RespBean uploadFile(MultipartFile multipartFile, String filePath ) throws IOException {
        return RespBean.sucess("上传文件成功", weixinFileService.uploadFile(multipartFile.getBytes(), filePath, multipartFile.getOriginalFilename()));
    }

    @GetMapping("dwonFile")
    @ApiOperation("下载文件信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="fileId", dataType = "Long", value = "文件id", paramType = "query",example = "1"),
    })
    public RespBean dwonFile(Long fileId){
        return RespBean.sucess("下载成功", weixinFileService.downFile(fileId));
    }
}
