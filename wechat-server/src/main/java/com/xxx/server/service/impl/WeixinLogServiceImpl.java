package com.xxx.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.mapper.WeixinLogMapper;
import com.xxx.server.pojo.WeixinLog;
import com.xxx.server.service.IWeixinLogService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-08-29
 */
@Service
public class WeixinLogServiceImpl extends ServiceImpl<WeixinLogMapper, WeixinLog> implements IWeixinLogService {

}
