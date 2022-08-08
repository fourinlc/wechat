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
import com.xxx.server.service.IWeixinBaseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
                        .setUpdateTime(LocalDateTime.now())
                        .setLastTime(LocalDateTime.now())
                        .setCreateTime(LocalDateTime.now());
                weixinBaseInfoMapper.insert(weixinBaseInfo);
            } else {
                weixinBaseInfo.setLastTime(weixinBaseInfo.getUpdateTime())
                        .setUpdateTime(LocalDateTime.now());
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
        Object obj = WechatApiHelper.LOG_OUT.invoke(null,logOutMap);
        Map entity = (Map)obj;
        if (entity.get("Code").equals(200)){
            return RespBean.sucess("退出成功",obj);
        } else {
            return RespBean.error("退出失败",obj);
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("CurrentWxcontactSeq",0);
        jsonObject.put("CurrentChatRoomContactSeq",0);
        MultiValueMap<String,String> getContactListMap = new LinkedMultiValueMap<>();
        getContactListMap.add("key", key);
        JSONObject resultJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CONTACT_LIST.invoke(jsonObject,getContactListMap)));
        JSONArray userNameList = resultJson.getJSONObject("Data").getJSONObject("ContactList").getJSONArray("contactUsernameList");
        ArrayList<String> friendList = new ArrayList<>();
        ArrayList<String> chatRoomList = new ArrayList<>();
        for (Object o : userNameList) {
            String userName = o.toString();
            if (userName.startsWith("wxid_")) {
                friendList.add(userName);
            } else {
                chatRoomList.add(userName);
            }
        }
        MultiValueMap<String,String> getDetailsListMap = new LinkedMultiValueMap<>();
        getDetailsListMap.add("key",key);
        JSONObject getDetailersObject = new JSONObject();
        getDetailersObject.put("UserNames",friendList);
        getDetailersObject.put("RoomWxIDList",chatRoomList);
        WechatApiHelper.GET_CONTACT_DETAILS_LIST.invoke(getDetailersObject,getDetailsListMap);
        return RespBean.sucess("doing...");
    }

    @Override
    public List<WeixinBaseInfo> queryList(){
        return baseMapper.selectList(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getState, 1).orderByDesc(WeixinBaseInfo::getCreateTime));
    }
}
