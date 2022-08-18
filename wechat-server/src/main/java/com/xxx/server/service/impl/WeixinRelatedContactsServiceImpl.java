package com.xxx.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.mapper.WeixinRelatedContactsMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinBaseInfo;
import com.xxx.server.pojo.WeixinRelatedContacts;
import com.xxx.server.service.IWeixinRelatedContactsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-08-09
 */
@Service
@AllArgsConstructor
@Slf4j
public class WeixinRelatedContactsServiceImpl extends ServiceImpl<WeixinRelatedContactsMapper, WeixinRelatedContacts> implements IWeixinRelatedContactsService {

    WeixinRelatedContactsMapper weixinRelatedContactsMapper;

    WeixinBaseInfoMapper weixinBaseInfoMapper;

    @Override
    public RespBean relatedFriends(String wxId, List<String> relatedWxIds) {
        MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class)
                .eq(WeixinRelatedContacts::getWxId, wxId));
        if (relatedContacts == null) {
            relatedContacts = new WeixinRelatedContacts();
            relatedContacts.setWxId(wxId)
                    .setRelated1("")
                    .setRelated2("");
            weixinRelatedContactsMapper.insert(relatedContacts);
        }

        for (String relatedWxId : relatedWxIds) {
            //判断是否已关联
            if (weixinRelatedContactsMapper.selectCount(Wrappers.lambdaQuery(WeixinRelatedContacts.class)
                    .eq(WeixinRelatedContacts::getRelated1, relatedWxId)
                    .or()
                    .eq(WeixinRelatedContacts::getRelated2, relatedWxId)
                    .or()
                    .eq(WeixinRelatedContacts::getWxId, relatedWxId)) > 0) {
                errorMap.add(relatedWxId, "该账号已有关联账号，请先取消");
                continue;
            }
            //判断是否登录过
            if (weixinBaseInfoMapper.selectCount(Wrappers.lambdaQuery(WeixinBaseInfo.class)
                    .eq(WeixinBaseInfo::getWxId, relatedWxId)) == 0) {
                errorMap.add(relatedWxId, "该账号未登录，请先登录");
                continue;
            }
            if (relatedContacts.getRelated1().equals("")) {
                relatedContacts.setRelated1(relatedWxId);
            } else if (relatedContacts.getRelated2().equals("")) {
                relatedContacts.setRelated2(relatedWxId);
            } else {
                errorMap.add(relatedWxId, "关联位已满,该微信号关联失败");
            }
        }
        weixinRelatedContactsMapper.updateById(relatedContacts);
        if (errorMap.isEmpty()) {
            return RespBean.sucess("关联成功");
        } else {
            return RespBean.sucess("关联失败", errorMap);
        }
    }

    @Override
    public RespBean getRelatedFriends(String wxId) {
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class)
                .eq(WeixinRelatedContacts::getWxId, wxId)
                .or()
                .eq(WeixinRelatedContacts::getRelated1, wxId)
                .or()
                .eq(WeixinRelatedContacts::getRelated2, wxId));
        if (relatedContacts == null) {
            return RespBean.error("未找到关联好友");
        }
        //获取微信名和头像
        Map<String, String> respInfoMap = new HashMap<>();
        respInfoMap.put("wxid", relatedContacts.getWxId());
        ArrayList<String> contactList = new ArrayList<>();
        if (!relatedContacts.getRelated1().equals("")) {
            contactList.add(relatedContacts.getRelated1());
            respInfoMap.put("related1", relatedContacts.getRelated1());
        }
        if (!relatedContacts.getRelated2().equals("")) {
            contactList.add(relatedContacts.getRelated2());
            respInfoMap.put("related2", relatedContacts.getRelated2());
        }

        if (contactList.size() != 0) {
            if (respInfoMap.containsKey("related1")) {
                WeixinBaseInfo weixinBaseInfo = weixinBaseInfoMapper.selectById(respInfoMap.get("related1"));
                respInfoMap.put("nickName1", weixinBaseInfo.getNickname());
                respInfoMap.put("smallHeadImgUrl1", weixinBaseInfo.getHeadImgUrl());
            }
            if (respInfoMap.containsKey("related2")) {
                WeixinBaseInfo weixinBaseInfo = weixinBaseInfoMapper.selectById(respInfoMap.get("related2"));
                respInfoMap.put("nickName2", weixinBaseInfo.getNickname());
                respInfoMap.put("smallHeadImgUrl2", weixinBaseInfo.getHeadImgUrl());
            }
        }
        return RespBean.sucess("查询成功", respInfoMap);
    }

    @Override
    public RespBean cancelRelatedFriends(String wxId, List<String> relatedWxIds) {
        MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();
        WeixinRelatedContacts relatedContacts = weixinRelatedContactsMapper.selectById(wxId);
        for (String relatedWxId : relatedWxIds) {
            if (relatedContacts.getRelated1().equals(relatedWxId)) {
                relatedContacts.setRelated1("");
            } else if (relatedContacts.getRelated2().equals(relatedWxId)) {
                relatedContacts.setRelated2("");
            } else {
                errorMap.add(relatedWxId, "取消关联失败");
            }
        }
        weixinRelatedContactsMapper.updateById(relatedContacts);
        if (errorMap.isEmpty()) {
            return RespBean.sucess("取消关联成功");
        } else {
            return RespBean.sucess("取消关联失败", errorMap);
        }
    }

    // 获取同级子号信息
    public List<WeixinBaseInfo> queryRelatedList(String wxId) {
        // 首先获取主账号id信息以及另外一个主号
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsMapper.selectOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class).eq(WeixinRelatedContacts::getRelated1, wxId).or().eq(WeixinRelatedContacts::getRelated2, wxId));
        if (weixinRelatedContacts != null) {
            String related1 = weixinRelatedContacts.getRelated1();
            String related2 = weixinRelatedContacts.getRelated2();
            if (StrUtil.isNotEmpty(related1) && StrUtil.isNotEmpty(related2)) {
                log.info("该账号存在同级子号：{}", wxId);
                // 展示同级子号信息
                return weixinBaseInfoMapper.selectBatchIds(Lists.newArrayList(related1, related2));
            }
        }
        List<WeixinBaseInfo> weixinBaseInfos = weixinBaseInfoMapper.selectBatchIds(Lists.newArrayList(wxId));
        Assert.isTrue(weixinBaseInfos.size() == 1, "该微信号未登录系统");
        return weixinBaseInfos;
    }
}
