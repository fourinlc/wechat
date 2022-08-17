package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.mapper.WeixinRelatedContactsMapper;
import com.xxx.server.service.IWeixinRelatedContactsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-08-09
 */
@Service
public class WeixinRelatedContactsServiceImpl extends ServiceImpl<WeixinRelatedContactsMapper, WeixinRelatedContacts> implements IWeixinRelatedContactsService {
    @Autowired
    WeixinRelatedContactsMapper weixinRelatedContactsMapper;

    @Autowired
    WeixinBaseInfoMapper weixinBaseInfoMapper;

    @Override
    public RespBean relatedFriends(String wxId, List<String> relatedWxIds) {
        MultiValueMap<String,String> errorMap = new LinkedMultiValueMap<>();
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class)
                .eq(WeixinRelatedContacts::getWxId,wxId));
        if (relatedContacts == null) {
            relatedContacts = new WeixinRelatedContacts();
            relatedContacts.setWxId(wxId)
                    .setRelated1("")
                    .setRelated2("");
            weixinRelatedContactsMapper.insert(relatedContacts);
        }

        for(String relatedWxId : relatedWxIds){
            if (weixinRelatedContactsMapper.selectCount(Wrappers.lambdaQuery(WeixinRelatedContacts.class)
                    .eq(WeixinRelatedContacts::getRelated1,relatedWxId)
                    .or()
                    .eq(WeixinRelatedContacts::getRelated2,relatedWxId)
                    .or()
                    .eq(WeixinRelatedContacts::getWxId,relatedWxId)) > 0){
                errorMap.add(relatedWxId,"该账号已有关联账号，请先取消");
                continue;
            }
            if (relatedContacts.getRelated1().equals("")){
                relatedContacts.setRelated1(relatedWxId);
            } else if(relatedContacts.getRelated2().equals("")) {
                relatedContacts.setRelated2(relatedWxId);
            } else {
                errorMap.add(relatedWxId,"关联位已满,该微信号关联失败");
            }
        }
        weixinRelatedContactsMapper.updateById(relatedContacts);
        if (errorMap.isEmpty()){
            return RespBean.sucess("关联成功");
        } else {
            return RespBean.sucess("关联失败",errorMap);
        }
    }

    @Override
    public RespBean getRelatedFriends(String wxId) {
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class).eq(WeixinRelatedContacts::getWxId,wxId));
        //获取微信名和头像
        Map<String,String> respInfoMap = new HashMap<>();
        respInfoMap.put("wxid",relatedContacts.getWxId());
        ArrayList<String> contactList = new ArrayList<>();
        if (!relatedContacts.getRelated1().equals("")) {
            contactList.add(relatedContacts.getRelated1());
            respInfoMap.put("related1",relatedContacts.getRelated1());
        }
        if (!relatedContacts.getRelated2().equals("")) {
            contactList.add(relatedContacts.getRelated2());
            respInfoMap.put("related2",relatedContacts.getRelated2());
        }

        if (contactList.size() != 0) {
            if (respInfoMap.containsKey("related1")) {
                WeixinBaseInfo weixinBaseInfo =  weixinBaseInfoMapper.selectById(respInfoMap.get("related1"));
                respInfoMap.put("nickName1",weixinBaseInfo.getNickname());
                respInfoMap.put("smallHeadImgUrl1",weixinBaseInfo.getHeadImgUrl());
            }
            if (respInfoMap.containsKey("related2")) {
                WeixinBaseInfo weixinBaseInfo =  weixinBaseInfoMapper.selectById(respInfoMap.get("related2"));
                respInfoMap.put("nickName2",weixinBaseInfo.getNickname());
                respInfoMap.put("smallHeadImgUrl2",weixinBaseInfo.getHeadImgUrl());
            }
        }
        return RespBean.sucess("查询成功",respInfoMap);
    }

    @Override
    public RespBean cancelRelatedFriends(String wxId, List<String> relatedWxIds) {
        MultiValueMap<String,String> errorMap = new LinkedMultiValueMap<>();
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectById(wxId);
        for(String relatedWxId : relatedWxIds){
            if (relatedContacts.getRelated1().equals(relatedWxId)) {
                relatedContacts.setRelated1("");
            } else if (relatedContacts.getRelated2().equals(relatedWxId)) {
                relatedContacts.setRelated2("");
            } else {
                errorMap.add(relatedWxId,"取消关联失败");
            }
        }
        weixinRelatedContactsMapper.updateById(relatedContacts);
        if (errorMap.isEmpty()){
            return RespBean.sucess("取消关联成功");
        } else {
            return RespBean.sucess("取消关联失败",errorMap);
        }
    }
}
