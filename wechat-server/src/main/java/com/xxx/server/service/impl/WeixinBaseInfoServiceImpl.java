package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.mapper.WeixinRelatedContactsMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.service.IWeixinBaseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
            } else {
                map.clear();
                map.add("key",key);
                JSONObject profileJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_PROFILE.invoke(null,map)));
                if(profileJson.containsKey("Code")){
                    code = profileJson.getString("Code");
                }else {
                    code = profileJson.getString("code");
                }
                if (code.equals("200")){
                    if (profileJson.getJSONObject("data").getJSONObject("userInfoExt").containsKey("smallHeadImgUrl")){
                        headImgUrl = profileJson.getJSONObject("data").getJSONObject("userInfoExt").getString("smallHeadImgUrl");
                    }
                }
            }
            WeixinBaseInfo weixinBaseInfo;
            weixinBaseInfo = weixinBaseInfoMapper.selectOne(new QueryWrapper<WeixinBaseInfo>().eq("wx_id",resultJson.getJSONObject("Data").get("wxid")));
            if (weixinBaseInfo == null) {
                weixinBaseInfo = new WeixinBaseInfo();
                weixinBaseInfo.setKey(key)
                        .setUuid(uuid)
                        .setWxId(resultJson.getJSONObject("Data").get("wxid").toString())
                        .setNickname(resultJson.getJSONObject("Data").get("nick_name").toString())
                        .setState(resultJson.getJSONObject("Data").get("state").toString())
                        .setUpdateTime(LocalDateTime.now())
                        .setLastTime(LocalDateTime.now())
                        .setCreateTime(LocalDateTime.now())
                        .setHeadImgUrl(headImgUrl);
                weixinBaseInfoMapper.insert(weixinBaseInfo);
            } else {
                weixinBaseInfo.setLastTime(weixinBaseInfo.getUpdateTime())
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
            return RespBean.sucess("退出成功",resultJson);
        } else {
            return RespBean.error("退出失败",resultJson);
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("CurrentWxcontactSeq",0);
        jsonObject.put("CurrentChatRoomContactSeq",0);
        MultiValueMap<String,String> getContactListMap = new LinkedMultiValueMap<>();
        getContactListMap.add("key", key);
        JSONObject resultJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CONTACT_LIST.invoke(jsonObject,getContactListMap)));
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else {
            code = resultJson.getString("code");
        }
        if (!code.equals("200")){
            return RespBean.error("获取好友列表失败",resultJson);
        }
        //通过wxid获取详细信息
        JSONArray userNameList = resultJson.getJSONObject("Data").getJSONObject("ContactList").getJSONArray("contactUsernameList");
        ArrayList<String> friendList = new ArrayList<>();
        ArrayList<String> contactList = new ArrayList<>();
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
        MultiValueMap<String,String> getDetailsListMap = new LinkedMultiValueMap<>();
        getDetailsListMap.add("key",key);
        JSONObject getDetailersObject = new JSONObject();
        getDetailersObject.put("UserNames",contactList);
        JSONObject detailsJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CONTACT_DETAILS_LIST.invoke(getDetailersObject,getDetailsListMap)));
        if(detailsJson.containsKey("Code")){
            code = detailsJson.getString("Code");
        }else if(detailsJson.containsKey("code")){
            code = detailsJson.getString("code");
        }
        if(!code.equals("200")){
            return RespBean.error("获取好友详情失败",detailsJson);
        }
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsMapper.selectById(wxid);
        ArrayList<String> relatedList = new ArrayList<>();
        if (weixinRelatedContacts != null) {
            relatedList.add(weixinRelatedContacts.getRelated1());
            relatedList.add(weixinRelatedContacts.getRelated2());
        }
        //过滤出好友和群
        JSONArray detailsList = detailsJson.getJSONObject("Data").getJSONArray("contactList");
        MultiValueMap<String,ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = new LinkedMultiValueMap<>();
        ArrayList<WeixinContactDetailedInfo> chatRoomDetaileList = new ArrayList<>();
        ArrayList<WeixinContactDetailedInfo> friendDetaileList = new ArrayList<>();
        for (Object o : detailsList) {
            JSONObject detailJson = JSONObject.parseObject(o.toString());
            WeixinContactDetailedInfo contactDetailedInfo = new WeixinContactDetailedInfo();
            contactDetailedInfo.setWxId(detailJson.getString("userName").substring(8,detailJson.getString("userName").length()-2));
            System.out.println(detailJson.getString("nickName").length());
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
                friendDetaileList.add(contactDetailedInfo);
            } else {
                contactDetailedInfo.setChatRoomOwner(detailJson.getString("chatRoomOwner"));
                chatRoomDetaileList.add(contactDetailedInfo);
            }
        }
        contactDetailedInfoMap.add("friendsDetail",friendDetaileList);
        contactDetailedInfoMap.add("chatRoomDetaile",chatRoomDetaileList);
        return RespBean.sucess("查询成功",contactDetailedInfoMap);
    }

    @Override
    public List<WeixinBaseInfo> queryList(){
        return baseMapper.selectList(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getState, 1).orderByDesc(WeixinBaseInfo::getCreateTime));
    }
}
