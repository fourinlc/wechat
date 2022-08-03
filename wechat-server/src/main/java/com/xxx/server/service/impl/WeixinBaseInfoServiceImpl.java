package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinUser;
import com.xxx.server.service.IWeixinBaseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
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
public class WeixinBaseInfoServiceImpl extends ServiceImpl<WeixinBaseInfoMapper, WeixinBaseInfo> implements IWeixinBaseInfoService {

    @Autowired
    WeixinBaseInfoMapper weixinBaseInfoMapper;
    @Override
    public RespBean getLoginQrcode() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Proxy","");
        Object obj = WechatApiHelper.GET_LOGIN_QRCODE_NEW.invoke(jsonObject,null);
        Map entity = (Map)obj;
        if (entity.get("Code").equals(200)){
            return RespBean.sucess("获取登录二维码成功",obj);
        } else {
            return RespBean.error("获取登录二维码失败",obj);
        }
    }

    @Override
    public RespBean checkLoginStatus(String key, String uuid) {
        MultiValueMap<String,String> map = new LinkedMultiValueMap<>();
        map.add("key", key);
        map.add("uuid", uuid);
        Object obj = WechatApiHelper.CHECK_LOGIN_STATUS.invoke(null,map);
        Map entity = (Map)obj;
        if (entity.get("Code").equals(200)){
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoMapper.selectOne(new QueryWrapper<WeixinBaseInfo>().eq("wx_id",entity.get("wxid")));
            if (weixinBaseInfo == null) {
                weixinBaseInfo = new WeixinBaseInfo();
                weixinBaseInfo.setKey(key)
                        .setUuid(uuid)
                        .setWxId(entity.get("wxid").toString())
                        .setNickname(entity.get("nick_name").toString())
                        .setState(entity.get("state").toString())
                        .setCreateTime(LocalDateTime.now());
                weixinBaseInfoMapper.insert(weixinBaseInfo);
            }
            return RespBean.sucess("登录成功",weixinBaseInfo);
        } else {
            return RespBean.error("还未登录",obj);
        }
    }

    @Override
    public RespBean modifyRemarkName(String wxId, String remarkName) {
        WeixinBaseInfo weixinBaseInfo = new WeixinBaseInfo();
        weixinBaseInfo.setWxId(wxId);
        weixinBaseInfo.setRemarkName(remarkName);
        int result = weixinBaseInfoMapper.updateById(weixinBaseInfo);
        if (result == 0){
            return RespBean.sucess("修改失败,该用户不存在");
        } else
        {
            return RespBean.sucess("修改成功");
        }
    }
}
