package com.xxx.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxx.server.config.security.JwtTokenUtil;
import com.xxx.server.mapper.WeixinUserMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinUser;
import com.xxx.server.service.IWeixinUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

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
    @Autowired
    private WeixinUserMapper weixinUserMapper;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    @Override
    public RespBean login(String userName, String passWord, HttpServletRequest request){
        //登录
        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        if (null == userDetails || passwordEncoder.matches(passWord,userDetails.getPassword())){
            return RespBean.error("用户名或密码不正确");
        }
        if (!userDetails.isEnabled()){
            return RespBean.error("账号被禁用");
        }
        //更新security登录用户对象
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //生成token
        String token = jwtTokenUtil.generateToken(userDetails);
        Map<String,String> tokenMap = new HashMap<>();
        tokenMap.put("token",token);
        tokenMap.put("tokenHead",tokenHead);
        return RespBean.sucess("登录成功",tokenMap);
    }

    @Override
    public WeixinUser getWeixinUserByUserName(String userName) {
        return weixinUserMapper.selectOne(new QueryWrapper<WeixinUser>().eq("user_name",userName));
    }

    @Override
    public RespBean register(String userName, String passWord) {
        WeixinUser weixinUser = new WeixinUser();
        weixinUser.setUserName(userName)
                .setUserPassWord(passWord);
        if (weixinUserMapper.selectOne(new QueryWrapper<WeixinUser>().eq("user_name",userName)).getUsername()
                .equals(weixinUser.getUsername())){
            return RespBean.error("注册失败:用户名已存在");
        }
        int result = weixinUserMapper.insert(weixinUser);
        return RespBean.sucess("注册成功",result);
    }
}
