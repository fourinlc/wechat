package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.service.IWeixinBaseInfoService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public RespBean getLoginQrcode(){
        return weixinBaseInfoService.getLoginQrcode();
    }

    @ApiOperation(value = "登录检测")
    @PostMapping("/checkLoginStatus")
    public RespBean checkLoginStatus(@RequestBody WeixinBaseInfo weixinBaseInfo){
        return weixinBaseInfoService.checkLoginStatus(weixinBaseInfo.getKey(),weixinBaseInfo.getUuid());
    }

    @ApiOperation(value = "修改备注")
    @PostMapping("/modifyRemarkName")
    public RespBean modifyRemarkName(String wxId, String remarkName){
        return weixinBaseInfoService.modifyRemarkName(wxId,remarkName);
    }

    @ApiOperation(value = "获取好友和群列表")
    @GetMapping("/getFriends")
    public RespBean getFriendsAndChatRooms(String key){
        return weixinBaseInfoService.getFriendsAndChatRooms(key);
    }
}
