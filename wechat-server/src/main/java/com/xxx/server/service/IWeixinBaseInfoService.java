package com.xxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinBaseInfoService extends IService<WeixinBaseInfo> {

    RespBean getLoginQrcode();

    RespBean checkLoginStatus(String key, String uuid);

    RespBean modifyRemarkName(String wxId, String remarkName);

    RespBean getFriendsAndChatRooms(String key, String wxid, boolean refresh);

    List<WeixinBaseInfo> queryList();

    RespBean logOut(String key);

    RespBean deviceLogin(String wxid, String passWord);
}
