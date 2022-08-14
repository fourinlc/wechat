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

import java.util.ArrayList;
import java.util.List;

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
                    .eq(WeixinRelatedContacts::getRelated2,relatedWxId)) > 0){
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
        WeixinBaseInfo weixinBaseInfo =  weixinBaseInfoMapper.selectById(wxId);
        ArrayList<String> contactList = new ArrayList<>();
        contactList.add(relatedContacts.getRelated1());
        contactList.add(relatedContacts.getRelated2());
        MultiValueMap<String,String> getDetailsListMap = new LinkedMultiValueMap<>();
        getDetailsListMap.add("key",weixinBaseInfo.getKey());
        JSONObject getDetailersObject = new JSONObject();
        getDetailersObject.put("UserNames",contactList);
        JSONObject detailsJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CONTACT_DETAILS_LIST.invoke(getDetailersObject,getDetailsListMap)));
        String code = "";
        if(detailsJson.containsKey("Code")){
            code = detailsJson.getString("Code");
        }else if(detailsJson.containsKey("code")){
            code = detailsJson.getString("code");
        }
        if(!code.equals("200")){
            return RespBean.error("获取好友详情失败",detailsJson);
        }
        MultiValueMap<String,String> respInfoMap = new LinkedMultiValueMap<>();
        JSONArray detailsList = detailsJson.getJSONObject("Data").getJSONArray("contactList");
        JSONObject detailJson1 = JSONObject.parseObject(detailsList.get(0).toString());
        JSONObject detailJson2 = JSONObject.parseObject(detailsList.get(1).toString());
        respInfoMap.add("wxid",relatedContacts.getWxId());
        respInfoMap.add("related1",relatedContacts.getRelated1());
        respInfoMap.add("related2",relatedContacts.getRelated2());
        respInfoMap.add("nickName1",detailJson1.getString("nickName").substring(8, detailJson1.getString("nickName").length() - 2));
        respInfoMap.add("nickName2",detailJson2.getString("nickName").substring(8, detailJson1.getString("nickName").length() - 2));
        respInfoMap.add("smallHeadImgUrl1",detailJson1.getString("smallHeadImgUrl"));
        respInfoMap.add("smallHeadImgUrl2",detailJson2.getString("smallHeadImgUrl"));
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
