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

    @ApiOperation(value = "退出登录")
    @PostMapping("/logOut")
    public RespBean logOut(String key){
        return weixinBaseInfoService.logOut(key);
    }

    @ApiOperation(value = "修改备注")
    @PostMapping("/modifyRemarkName")
    public RespBean modifyRemarkName(String wxId, String remarkName){
        return weixinBaseInfoService.modifyRemarkName(wxId,remarkName);
    }

    @ApiOperation(value = "获取好友和群列表")
    @GetMapping("/getFriends")
    public RespBean getFriendsAndChatRooms(String key, String wxid, boolean refresh){
        return weixinBaseInfoService.getFriendsAndChatRooms(key,wxid,refresh);
    }

    @ApiOperation("获取在线好友列表")
    @GetMapping("queryList")
    public RespBean query(){
        return RespBean.sucess("查询成功", weixinBaseInfoService.queryList());
    }

    @ApiOperation(value = "A16/62登录")
    @PostMapping("/DeviceLogin")
    public RespBean deviceLogin(String wxid, String passWord){
        return weixinBaseInfoService.deviceLogin(wxid,passWord);
    }
}
