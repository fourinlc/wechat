package com.xxx.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.mapper.WeixinTemplateSendDetailMapper;
import com.xxx.server.pojo.*;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.service.IWeixinRelatedContactsService;
import com.xxx.server.service.IWeixinTemplateSendDetailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-08-16
 */
@Service
@AllArgsConstructor
@Slf4j
public class WeixinTemplateSendDetailServiceImpl extends ServiceImpl<WeixinTemplateSendDetailMapper, WeixinTemplateSendDetail> implements IWeixinTemplateSendDetailService {

    private IWeixinRelatedContactsService weixinRelatedContactsService;

    private IWeixinBaseInfoService weixinBaseInfoService;

    // 获取群发或者待群发列表
    public List<WeixinTemplateSendDetail> queryList(String wxId, boolean refresh) {
        List<WeixinBaseInfo> weixinBaseInfos = weixinRelatedContactsService.queryRelatedList(wxId);
        WeixinRelatedContacts weixinRelatedContacts = weixinRelatedContactsService.getOne(Wrappers.lambdaQuery(WeixinRelatedContacts.class).eq(WeixinRelatedContacts::getRelated1, wxId).or().eq(WeixinRelatedContacts::getRelated2, wxId));
        List<WeixinContactDetailedInfo> weixinContactDetailedInfos = Lists.newArrayList();

        if (weixinBaseInfos.size() == 1) {
            List<WeixinTemplateSendDetail> weixinTemplateSendDetails = Lists.newArrayList();
            WeixinBaseInfo weixinBaseInfo = weixinBaseInfos.get(0);
            // 暂时未关联或者已关联但只有一个账号
            RespBean friendsAndChatRooms = weixinBaseInfoService.getFriendsAndChatRooms(weixinBaseInfo.getKey(), weixinBaseInfo.getUuid(), true);
            MultiValueMap<String, ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = (MultiValueMap) friendsAndChatRooms.getObj();
            List<ArrayList<WeixinContactDetailedInfo>> chatRoomDetaile = contactDetailedInfoMap.get("chatRoomDetaile");
            if (chatRoomDetaile.size() > 0) {
                ArrayList<WeixinContactDetailedInfo> weixinContactDetailedInfo = chatRoomDetaile.get(0);
                weixinContactDetailedInfos.addAll(weixinContactDetailedInfo);
            }
            weixinContactDetailedInfos.forEach(weixinContactDetailedInfo -> {
                // 构建具体的数据
                WeixinTemplateSendDetail weixinTemplateSendDetail = new WeixinTemplateSendDetail()
                        .setChatRoomId(weixinContactDetailedInfo.getWxId())
                        .setChatRoomName(weixinContactDetailedInfo.getUserName());
                weixinTemplateSendDetails.add(weixinTemplateSendDetail);
            });
            return weixinTemplateSendDetails;
        } else {
            // 存在关联信息且已关联,先去存储数据表中获取是否已经生成模板信息
            List<WeixinTemplateSendDetail> weixinTemplateSendDetails = getBaseMapper().selectList(
                    Wrappers.lambdaQuery(WeixinTemplateSendDetail.class)
                            .eq(WeixinTemplateSendDetail::getWxId, weixinRelatedContacts.getWxId())
                            .eq(WeixinTemplateSendDetail::getCreateTime, new Date()));
            if (weixinTemplateSendDetails.size() < 1) {
                // 构造模板参数并入库
                // 查询两个微信的所有群信息详情
                for (WeixinBaseInfo weixinBaseInfo : weixinBaseInfos) {
                    // 校验账号登录情况
                    if (!StrUtil.equals(weixinBaseInfo.getState(), "1")) continue;
                    RespBean friendsAndChatRooms = weixinBaseInfoService.getFriendsAndChatRooms(weixinBaseInfo.getKey(), weixinBaseInfo.getUuid(), refresh);
                    MultiValueMap<String, ArrayList<WeixinContactDetailedInfo>> contactDetailedInfoMap = (MultiValueMap) friendsAndChatRooms.getObj();
                    List<ArrayList<WeixinContactDetailedInfo>> chatRoomDetaile = contactDetailedInfoMap.get("chatRoomDetaile");
                    if (chatRoomDetaile.size() > 0) {
                        ArrayList<WeixinContactDetailedInfo> weixinContactDetailedInfo = chatRoomDetaile.get(0);
                        weixinContactDetailedInfos.addAll(weixinContactDetailedInfo);
                    }
                }
                // 去重相同的数据
                weixinContactDetailedInfos.stream().distinct().forEach(weixinContactDetailedInfo -> {
                    // 构建具体的数据
                    WeixinTemplateSendDetail weixinTemplateSendDetail = new WeixinTemplateSendDetail()
                            .setChatRoomId(weixinContactDetailedInfo.getWxId())
                            .setChatRoomName(weixinContactDetailedInfo.getUserName());
                    weixinTemplateSendDetails.add(weixinTemplateSendDetail);
                });
            }
            return weixinTemplateSendDetails;
        }
    }
}
