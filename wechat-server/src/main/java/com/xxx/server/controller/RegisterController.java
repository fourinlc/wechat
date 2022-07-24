package com.xxx.server.controller;

import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUserLoginParam;
import com.xxx.server.service.IWeixinUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "RegisterController")
@RestController
public class RegisterController {
    @Autowired
    private IWeixinUserService weixinUserService;

    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public RespBean register(@RequestBody WeixinUserLoginParam userLoginParam){
        return weixinUserService.register(userLoginParam.getUserName(),userLoginParam.getPassWord());
    }
}
