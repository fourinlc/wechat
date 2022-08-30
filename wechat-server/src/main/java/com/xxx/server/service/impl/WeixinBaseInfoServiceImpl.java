package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.mapper.WeixinRelatedContactsMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.IWeixinBaseInfoService;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@CacheConfig(cacheNames = "contacts")
public class WeixinBaseInfoServiceImpl extends ServiceImpl<WeixinBaseInfoMapper, WeixinBaseInfo> implements IWeixinBaseInfoService {

    @Autowired
    WeixinRelatedContactsMapper weixinRelatedContactsMapper;
    @Autowired
    WeixinBaseInfoMapper weixinBaseInfoMapper;

    @Override
    public RespBean getLoginQrcode() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Proxy","");
        JSONObject resultJson = JSONObject.parseObject(WechatApiHelper.GET_LOGIN_QRCODE_NEW.invoke(jsonObject,null).toString());
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else {
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            return RespBean.sucess("获取登录二维码成功",resultJson);
        } else {
            return RespBean.error("获取登录二维码失败",resultJson);
        }
    }

    @Override
    public RespBean checkLoginStatus(String key, String uuid) {
        MultiValueMap<String,String> map = new LinkedMultiValueMap<>();
        map.add("key", key);
        map.add("uuid", uuid);
        JSONObject resultJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.CHECK_LOGIN_STATUS.invoke(null,map)));
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else {
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            String headImgUrl = "";
            if (resultJson.getJSONObject("Data").containsKey("head_img_url")){
                headImgUrl = resultJson.getJSONObject("Data").getString("head_img_url");
            }
            WeixinBaseInfo weixinBaseInfo;
            weixinBaseInfo = weixinBaseInfoMapper.selectOne(new QueryWrapper<WeixinBaseInfo>().eq("wx_id",resultJson.getJSONObject("Data").get("wxid")));
            if (weixinBaseInfo == null) {
                weixinBaseInfo = new WeixinBaseInfo();
                weixinBaseInfo.setKey(key)
                        .setUuid(uuid)
                        .setWxId(resultJson.getJSONObject("Data").get("wxid").toString())
                        .setNickname(resultJson.getJSONObject("Data").get("nick_name").toString())
                        .setState("1")
                        .setUpdateTime(LocalDateTime.now())
                        .setLastTime(LocalDateTime.now())
                        .setCreateTime(LocalDateTime.now())
                        .setHeadImgUrl(headImgUrl);
                weixinBaseInfoMapper.insert(weixinBaseInfo);
            } else {
                weixinBaseInfo
                        .setKey(key)
                        .setState("1")
                        .setLastTime(weixinBaseInfo.getUpdateTime())
                        .setUpdateTime(LocalDateTime.now())
                        .setHeadImgUrl(headImgUrl);
                weixinBaseInfoMapper.updateById(weixinBaseInfo);
            }
            return RespBean.sucess("登录成功",weixinBaseInfo);
        } else {
            return RespBean.error("还未登录",resultJson);
        }
    }

    @Override
    public RespBean logOut(String key) {
        MultiValueMap<String,String> logOutMap = new LinkedMultiValueMap<>();
        logOutMap.add("key", key);
        JSONObject resultJson = JSONObject.parseObject(WechatApiHelper.LOG_OUT.invoke(null,logOutMap).toString());
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else{
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            WeixinBaseInfo weixinBaseInfo = new WeixinBaseInfo();
            weixinBaseInfo
                    .setKey(key)
                    .setState("0");
            weixinBaseInfoMapper.updateById(weixinBaseInfo);
            return RespBean.sucess("退出成功",resultJson);
        } else {
            return RespBean.error("退出失败",resultJson);
        }
    }

    @Override
    public RespBean get62Data(String key) {
        MultiValueMap<String,String> get62DataMap = new LinkedMultiValueMap<>();
        get62DataMap.add("key", key);
        JSONObject resultJson = JSONObject.parseObject(WechatApiHelper.GET_62_DATA.invoke(null,get62DataMap).toString());
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else{
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            return RespBean.sucess("查询成功",resultJson);
        } else {
            return RespBean.error("查询失败",resultJson);
        }
    }

    @Override
    public RespBean deviceLogin(DeviceLoginParam deviceLoginParam) {
        JSONObject jsonObject = JSONObject.parseObject(deviceLoginParam.toString());
        JSONObject resultJson = JSONObject.parseObject(WechatApiHelper.GET_LOGIN_QRCODE_NEW.invoke(jsonObject,null).toString());
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else {
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            return RespBean.sucess("登录成功",resultJson);
        } else {
            return RespBean.error("获取失败",resultJson);
        }
    }

    @Override
    public RespBean modifyRemarkName(String wxId, String remarkName) {
        WeixinBaseInfo weixinBaseInfo = new WeixinBaseInfo();
        weixinBaseInfo.setWxId(wxId)
                .setRemarkName(remarkName);
        int result = weixinBaseInfoMapper.updateById(weixinBaseInfo);
        if (result == 0){
            return RespBean.sucess("修改失败,该用户不存在");
        } else
        {
            return RespBean.sucess("修改成功");
        }
    }

    @Override
    @Cacheable(value = "contacts",key = "#wxid",unless = "#result?.code != 200")
    @CacheEvict(value = "contacts", key="#wxid", condition = "#refresh", beforeInvocation=true)
    public RespBean getFriendsAndChatRooms(String key, String wxid, boolean refresh) {
        //获取所有联系人wxid
        String code = null;
        int currentWxcontactSeq = 0;
        int currentChatRoomContactSeq = 0;
        int continueFlag = 0;
        int retry = 0;
        ArrayList<String> friendList = new ArrayList<>();
        ArrayList<String> contactList = new ArrayList<>();
        do {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("CurrentWxcontactSeq",currentWxcontactSeq);
            jsonObject.put("CurrentChatRoomContactSeq",currentChatRoomContactSeq);
            MultiValueMap<String,String> getContactListMap = new LinkedMultiValueMap<>();
            getContactListMap.add("key", key);
            JSONObject resultJson = null;
            try {
                resultJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CONTACT_LIST.invoke(jsonObject,getContactListMap)));
            } catch (Exception e) {
                e.printStackTrace();
                if (retry == 3) {
                    return RespBean.error("获取好友列表失败",resultJson);
                }
                retry++;
                continueFlag = 1;
                continue;
            }
            if(resultJson.containsKey("Code")){
                code = resultJson.getString("Code");
            }else {
                code = resultJson.getString("code");
            }
            if (!code.equals("200")){
                if (retry == 3) {
                    return RespBean.error("获取好友列表失败",resultJson);
                }
                retry++;
                continueFlag = 1;
                continue;
            }
            currentWxcontactSeq = resultJson.getJSONObject("Data").getJSONObject("ContactList").getInteger("currentWxcontactSeq");
            currentChatRoomContactSeq = resultJson.getJSONObject("Data").getJSONObject("ContactList").getInteger("currentChatRoomContactSeq");
            continueFlag = resultJson.getJSONObject("Data").getJSONObject("ContactList").getInteger("continueFlag");
            JSONArray userNameList = resultJson.getJSONObject("Data").getJSONObject("ContactList").getJSONArray("contactUsernameList");
            for (Object o : userNameList) {
                String userName = o.toString();
                if (userName.endsWith("@chatroom")) {
                    contactList.add(userName);
                } else if(!userName.equals("weixin") &&
                        !userName.equals("medianote") &&
                        !userName.equals("floatbottle") &&
                        !userName.equals("fmessage") &&
                        !userName.equals("filehelper") &&
                        !userName.startsWith("gh_")){
                    friendList.add(userName);
                    contactList.add(userName);
                }
            }
        } while (continueFlag == 1);
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsMapper.selectById(wxid);
        ArrayList<String> relatedList = new ArrayList<>();
        if (weixinRelatedContacts != null) {
            relatedList.add(weixinRelatedContacts.getRelated1());
            relatedList.add(weixinRelatedContacts.getRelated2());
        }
        //通过wxid获取详细信息
        MultiValueMap<String,String> getDetailsKeyMap = new LinkedMultiValueMap<>();
        getDetailsKeyMap.add("key",key);
        ArrayList<String> contactDetailsList = new ArrayList<>();
        Map<String,ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = new HashMap<>();
        contactDetailedInfoMap.put("friendsDetail",new ArrayList<>());
        contactDetailedInfoMap.put("chatRoomDetaile",new ArrayList<>());
        retry = 0;
        for (int i = 0; i < contactList.size(); i++) {
            contactDetailsList.add(contactList.get(i));
            if (contactDetailsList.size() == 7 || i == contactList.size() -1) {
                JSONObject getDetailersObject = new JSONObject();
                getDetailersObject.put("ChatRoomWxIdList",contactDetailsList);
                JSONObject detailsJson = null;
                do {
                    try {
                        detailsJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(getDetailersObject,getDetailsKeyMap)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (retry == 3) {
                            return RespBean.error("获取好友列表失败",detailsJson);
                        }
                        retry++;
                        continue;
                    }
                    if(detailsJson.containsKey("Code")){
                        code = detailsJson.getString("Code");
                    }else if(detailsJson.containsKey("code")){
                        code = detailsJson.getString("code");
                    }
                    if(!code.equals("200")){
                        if (retry == 3) {
                            return RespBean.error("获取好友详情失败",detailsJson);
                        }
                        retry++;
                        log.debug("获取好友详情失败："+detailsJson);
                        log.debug("获取好友详情重试："+retry);
                    } else {
                        break;
                    }
                }while (true);
                //过滤出好友和群
                JSONArray detailsList = detailsJson.getJSONObject("Data").getJSONArray("contactList");
                for (Object o : detailsList) {
                    JSONObject detailJson = JSONObject.parseObject(o.toString());
                    WeixinContactDetailedInfo contactDetailedInfo = new WeixinContactDetailedInfo();
                    contactDetailedInfo.setWxId(detailJson.getString("userName").substring(8,detailJson.getString("userName").length()-2));
                    if (detailJson.getString("nickName").length() > 7) {
                        contactDetailedInfo.setUserName(detailJson.getString("nickName").substring(8,detailJson.getString("nickName").length()-2));
                    }
                    contactDetailedInfo.setSex(detailJson.getString("sex"));
                    contactDetailedInfo.setSmallHeadImgUrl(detailJson.getString("smallHeadImgUrl"));
                    if (friendList.contains(contactDetailedInfo.getWxId())){
                        contactDetailedInfo.setSignature(detailJson.getString("signature"));
                        contactDetailedInfo.setBigHeadImgUrl(detailJson.getString("bigHeadImgUrl"));
                        if (relatedList.contains(contactDetailedInfo.getWxId())){
                            contactDetailedInfo.setRelated("1");
                        } else {
                            contactDetailedInfo.setRelated("0");
                        }
                        contactDetailedInfoMap.get("friendsDetail").add(contactDetailedInfo);
                    } else {
                        contactDetailedInfo.setChatRoomOwner(detailJson.getString("chatRoomOwner"));
                        contactDetailedInfoMap.get("chatRoomDetaile").add(contactDetailedInfo);
                    }
                }
                contactDetailsList.clear();
            }
        }
        return RespBean.sucess("查询成功",JSONObject.parseObject(JSONObject.toJSONString(contactDetailedInfoMap)));
    }

    @Override
    public List<WeixinBaseInfo> queryList(){
        return baseMapper.selectList(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getState, 1).orderByDesc(WeixinBaseInfo::getCreateTime));
    }

}
