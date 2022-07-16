package com.xxx.server.service.impl;

import com.xxx.server.mapper.WeixinUserMapper;
import com.xxx.server.pojo.WeixinUser;
import com.xxx.server.service.IWeixinUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

}
