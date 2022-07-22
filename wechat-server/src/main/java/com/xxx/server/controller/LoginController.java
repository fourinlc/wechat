package com.xxx.server.controller;

import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUser;
import com.xxx.server.pojo.WeixinUserLoginParam;
import com.xxx.server.service.IWeixinUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * @PackageName:com.xxx.server.controller
 * @ClassName:LoginController Description:登录
 * @author: lc
 * @date 2022/7/21 19:56
 */
@Api(tags = "LoginController")
@RestController
public class LoginController {

    @Autowired
    private IWeixinUserService weixinUserService;

    @ApiOperation(value = "登录之后返回token")
    @PostMapping("/login")
    public RespBean login(@RequestBody WeixinUserLoginParam userLoginParam, HttpServletRequest request){
        return weixinUserService.login(userLoginParam.getUserName(),userLoginParam.getPassWord(),request);
    }

    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/weixinUser/info")
    public WeixinUser getWeixinUserInfo(Principal principal){
        if (null==principal){
            return null;
        }
        String userName = principal.getName();
        WeixinUser weixinUser = weixinUserService.getWeixinUserByUserName(userName);
        weixinUser.setUserPassWord(null);
        return weixinUser;
    }

    @ApiOperation(value = "退出登录")
    @PostMapping("/logout")
    public RespBean logout(){
        return RespBean.sucess("注销成功");
    }

}
