package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.service.IWeixinBaseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class WeixinBaseInfoServiceImpl extends ServiceImpl<WeixinBaseInfoMapper, WeixinBaseInfo> implements IWeixinBaseInfoService {

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
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfoMapper.selectOne(new QueryWrapper<WeixinBaseInfo>().eq("wx_id",resultJson.getJSONObject("Data").get("wxid")));
            if (weixinBaseInfo == null) {
                weixinBaseInfo = new WeixinBaseInfo();
                weixinBaseInfo.setKey(key)
                        .setUuid(uuid)
                        .setWxId(resultJson.getJSONObject("Data").get("wxid").toString())
                        .setNickname(resultJson.getJSONObject("Data").get("nick_name").toString())
                        .setState(resultJson.getJSONObject("Data").get("state").toString())
                        .setCreateTime(LocalDateTime.now());
                weixinBaseInfoMapper.insert(weixinBaseInfo);
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
    public RespBean relatedFriends(String wxId, List<String> relatedWxIds) {
        MultiValueMap<String,String> errorMap = new LinkedMultiValueMap<>();
        for(String relatedWxId : relatedWxIds){
            if (weixinBaseInfoMapper.selectCount(Wrappers.lambdaQuery(WeixinBaseInfo.class)
                    .eq(WeixinBaseInfo::getWxId,relatedWxId)
                    .ne(WeixinBaseInfo::getParentWxid,"")) > 0){
                errorMap.add(relatedWxId,"该账号已有关联账号，请先取消");
                continue;
            }
            WeixinBaseInfo weixinBaseInfo = new WeixinBaseInfo();
            weixinBaseInfo.setWxId(relatedWxId)
                    .setParentWxid(wxId);
            if(weixinBaseInfoMapper.updateById(weixinBaseInfo) == 0){
                errorMap.add(relatedWxId,"关联失败");
            }
        }
        if (errorMap.isEmpty()){
            return RespBean.sucess("关联成功");
        } else {
            return RespBean.sucess("关联失败",errorMap);
        }
    }

    @Override
    public RespBean cancelRelatedFriends(List<String> relatedWxIds) {
        MultiValueMap<String,String> errorMap = new LinkedMultiValueMap<>();
        for(String relatedWxId : relatedWxIds){
            WeixinBaseInfo weixinBaseInfo = new WeixinBaseInfo();
            weixinBaseInfo.setWxId(relatedWxId)
                    .setParentWxid("");
            if(weixinBaseInfoMapper.updateById(weixinBaseInfo) == 0){
                errorMap.add(relatedWxId,"关联失败");
            }
        }
        if (errorMap.isEmpty()){
            return RespBean.sucess("取消关联成功");
        } else {
            return RespBean.sucess("取消关联失败",errorMap);
        }
    }

    @Override
    public RespBean getRelatedFriends(String wxId) {
        List<WeixinBaseInfo> weixinBaseInfos = weixinBaseInfoMapper.selectList(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getParentWxid,wxId));
        return RespBean.sucess("查询成功",weixinBaseInfos);
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
    public RespBean getFriendsAndChatRooms(String key) {
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
        //过滤出好友和群
        JSONArray detailsList = detailsJson.getJSONObject("Data").getJSONArray("contactList");
        MultiValueMap<String,ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = new LinkedMultiValueMap<>();
        ArrayList<WeixinContactDetailedInfo> chatRoomDetaileList = new ArrayList<>();
        ArrayList<WeixinContactDetailedInfo> friendDetaileList = new ArrayList<>();
        for (Object o : detailsList) {
            JSONObject detailJson = JSONObject.parseObject(o.toString());
            WeixinContactDetailedInfo contactDetailedInfo = new WeixinContactDetailedInfo();
            contactDetailedInfo.setWxId(detailJson.getString("userName").substring(8,detailJson.getString("userName").length()-2));
            contactDetailedInfo.setUserName(detailJson.getString("nickName").substring(8,detailJson.getString("nickName").length()-2));
            contactDetailedInfo.setSex(detailJson.getString("sex"));
            contactDetailedInfo.setSmallHeadImgUrl(detailJson.getString("smallHeadImgUrl"));
            if (friendList.contains(contactDetailedInfo.getWxId())){
                contactDetailedInfo.setSignature(detailJson.getString("signature"));
                contactDetailedInfo.setBigHeadImgUrl(detailJson.getString("bigHeadImgUrl"));
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
