package com.xxx.server.service.impl;

import com.xxx.server.mapper.WeixinUserMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUser;
import com.xxx.server.service.IWeixinUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@Service
public class WeixinUserServiceImpl extends ServiceImpl<WeixinUserMapper, WeixinUser> implements IWeixinUserService {

    @Override
    public RespBean login(String userName, String passWord, HttpServletRequest request){

        return null;
    }
}
