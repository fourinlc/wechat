package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.service.IWeixinBaseInfoService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  微信基础操作
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-base-info")
@AllArgsConstructor
@Slf4j
public class WeixinBaseInfoController{

    IWeixinBaseInfoService weixinBaseInfoService;

    @ApiOperation(value = "获取登录二维码")
    @PostMapping("/getLoginQrcode")
    RespBean getLoginQrcode(){
        return weixinBaseInfoService.getLoginQrcode();
    }

    @ApiOperation(value = "登錄檢測")
    @PostMapping("/checkLoginStatus")
    RespBean checkLoginStatus(@RequestBody WeixinBaseInfo weixinBaseInfo){
        return weixinBaseInfoService.checkLoginStatus(weixinBaseInfo.getKey(),weixinBaseInfo.getUuid());
    }
}
