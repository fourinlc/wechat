package com.xxx.server.controller;

import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUserLoginParam;
import com.xxx.server.service.IWeixinUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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
    public RespBean login(WeixinUserLoginParam userLoginParam, HttpServletRequest request){
        return weixinUserService.login(userLoginParam.getUserName(),userLoginParam.getPassWord(),request);
    }

}
