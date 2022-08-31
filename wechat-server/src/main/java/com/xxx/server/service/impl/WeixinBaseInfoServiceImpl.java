package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.mapper.WeixinRelatedContactsMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.service.IWeixinBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

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
@Slf4j
public class WeixinBaseInfoServiceImpl extends ServiceImpl<WeixinBaseInfoMapper, WeixinBaseInfo> implements IWeixinBaseInfoService {

    @Autowired
    WeixinRelatedContactsMapper weixinRelatedContactsMapper;
    @Autowired
    WeixinBaseInfoMapper weixinBaseInfoMapper;
    @Autowired
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

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
                String deviceId = get62Data(key);
                weixinBaseInfo.setKey(key)
                        .setUuid(uuid)
                        .setWxId(resultJson.getJSONObject("Data").get("wxid").toString())
                        .setNickname(resultJson.getJSONObject("Data").get("nick_name").toString())
                        .setState("1")
                        .setUpdateTime(LocalDateTime.now())
                        .setLastTime(LocalDateTime.now())
                        .setCreateTime(LocalDateTime.now())
                        .setHeadImgUrl(headImgUrl)
                        .setDeviceId(deviceId);
                weixinBaseInfoMapper.insert(weixinBaseInfo);
            } else {
                String deviceId = get62Data(key);
                weixinBaseInfo
                        .setKey(key)
                        .setState("1")
                        .setLastTime(weixinBaseInfo.getUpdateTime())
                        .setUpdateTime(LocalDateTime.now())
                        .setHeadImgUrl(headImgUrl)
                        .setDeviceId(deviceId);
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
            LambdaUpdateWrapper<WeixinBaseInfo> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(WeixinBaseInfo::getKey,key)
                    .set(WeixinBaseInfo::getState,"0");
            weixinBaseInfoMapper.update(null,lambdaUpdateWrapper);
            return RespBean.sucess("退出成功",resultJson);
        } else {
            return RespBean.error("退出失败",resultJson);
        }
    }

    public String get62Data(String key) {
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
            return resultJson.getString("Data");
        } else {
            return null;
        }
    }

    @Override
    public RespBean deviceLogin(String wxid, String passWord) {
        WeixinBaseInfo weixinBaseInfo;
        weixinBaseInfo = weixinBaseInfoMapper.selectOne(new QueryWrapper<WeixinBaseInfo>().eq("wx_id",wxid));
        if (weixinBaseInfo == null || weixinBaseInfo.getDeviceId() == null) {
            return RespBean.sucess("登录失败，请先进行一次扫码登录");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("DeviceId",weixinBaseInfo.getDeviceId());
        jsonObject.put("UserName",wxid);
        jsonObject.put("Password",passWord);
        JSONObject resultJson = JSONObject.parseObject(WechatApiHelper.DEVICE_LOGIN.invoke(jsonObject,null).toString());
        String code;
        if(resultJson.containsKey("Code")){
            code = resultJson.getString("Code");
        }else {
            code = resultJson.getString("code");
        }
        if (code.equals("200")){
            return RespBean.sucess("登录成功");
        } else {
            return RespBean.error("登录失败",resultJson);
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
        String code;
        int currentWxcontactSeq = 0;
        int currentChatRoomContactSeq = 0;
        int continueFlag;
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
        //通过wxid获取详细信息
        Map<String,ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = new HashMap<>();
        contactDetailedInfoMap.put("friendsDetail",new ArrayList<>());
        contactDetailedInfoMap.put("chatRoomDetaile",new ArrayList<>());
        ExecutorCompletionService<Integer> completionService = new ExecutorCompletionService<>(
                threadPoolTaskExecutor);

        List<List<String>> lists = Lists.partition(contactList, 7);
        lists.forEach(item -> {
            completionService.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    Map<String, ArrayList<WeixinContactDetailedInfo>> detailedInfo = getUserNameByWxId(key, item);
                    List<WeixinContactDetailedInfo> friendsDetails = detailedInfo.get("friendsDetail");
                    for (WeixinContactDetailedInfo weixinContactDetailedInfo : friendsDetails) {
                        contactDetailedInfoMap.get("friendsDetail").add(weixinContactDetailedInfo);
                    }
                    List<WeixinContactDetailedInfo> chatRoomDetailes = detailedInfo.get("chatRoomDetaile");
                    for (WeixinContactDetailedInfo weixinContactDetailedInfo : chatRoomDetailes) {
                        contactDetailedInfoMap.get("chatRoomDetaile").add(weixinContactDetailedInfo);
                    }
                    return null;
                }
            });
        });
        lists.forEach(item -> {
            try {
                completionService.take().get();
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e);
            }
        });
        log.info("好友详情查询成功");
        return RespBean.sucess("查询成功",JSONObject.parseObject(JSONObject.toJSONString(contactDetailedInfoMap)));
    }

    @Override
    public List<WeixinBaseInfo> queryList(){
        return baseMapper.selectList(Wrappers.lambdaQuery(WeixinBaseInfo.class).eq(WeixinBaseInfo::getState, 1).orderByDesc(WeixinBaseInfo::getCreateTime));
    }

    @Async("asyncServiceExecutor")
    public Map<String,ArrayList<WeixinContactDetailedInfo>> getUserNameByWxId(String key,List<String> wxIds){
        //通过wxid获取详细信息
        MultiValueMap<String,String> getDetailsKeyMap = new LinkedMultiValueMap<>();
        getDetailsKeyMap.add("key",key);
        Map<String,ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = new HashMap<>();
        contactDetailedInfoMap.put("friendsDetail",new ArrayList<>());
        contactDetailedInfoMap.put("chatRoomDetaile",new ArrayList<>());

        int retry = 0;
        String code = "";
        JSONObject getDetailersObject = new JSONObject();
        getDetailersObject.put("ChatRoomWxIdList",wxIds);
        JSONObject detailsJson;
        do {
            try {
                detailsJson = JSONObject.parseObject(JSONObject.toJSONString(WechatApiHelper.GET_CHAT_ROOM_INFO.invoke(getDetailersObject,getDetailsKeyMap)));
            } catch (Exception e) {
                e.printStackTrace();
                if (retry == 3) {
                    return null;
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
                    return null;
                }
                retry++;
                log.info("获取好友详情失败："+detailsJson);
                log.info("获取好友详情重试："+retry);
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
            if (contactDetailedInfo.getUserName() != null && contactDetailedInfo.getUserName().endsWith("@chatroom")){
                contactDetailedInfo.setSignature(detailJson.getString("signature"));
                contactDetailedInfo.setBigHeadImgUrl(detailJson.getString("bigHeadImgUrl"));
                contactDetailedInfoMap.get("friendsDetail").add(contactDetailedInfo);
            } else {
                contactDetailedInfo.setChatRoomOwner(detailJson.getString("chatRoomOwner"));
                contactDetailedInfoMap.get("chatRoomDetaile").add(contactDetailedInfo);
            }
        }
        return contactDetailedInfoMap;
    }
}
