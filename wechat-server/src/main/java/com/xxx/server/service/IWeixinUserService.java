package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUser;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinUserService extends IService<WeixinUser> {

    RespBean login(String userName, String passWord, HttpServletRequest request);

    WeixinUser getWeixinUserByUserName(String userName);

    RespBean register(String userName, String passWord);
}
