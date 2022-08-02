package com.xxx.server.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinGroupLinkDetailMapper;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * <p>
 *  微信群邀请相关
 * </p>
 *
 * @author qj
 * @since 2022-07-16
 */
@Service
@Slf4j
public class WeixinGroupLinkDetailServiceImpl extends ServiceImpl<WeixinGroupLinkDetailMapper, WeixinGroupLinkDetail> implements IWeixinGroupLinkDetailService {

    //TODO 增加间隔时间
    public boolean batchScanIntoUrlGroup(List<Long> linkIds){
        List<WeixinGroupLinkDetail> weixinGroupLinkDetails = baseMapper.selectBatchIds(linkIds);
        // 遍历进群
        // 缓存中获取进群间隔时间，分发mq延时任务进行消费
        for (WeixinGroupLinkDetail weixinGroupLinkDetail : weixinGroupLinkDetails) {
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap<>();
            // 获取对应的key值信息
            multiValueMap.add("key", weixinGroupLinkDetail.getKey());
            Object url = WechatApiHelper.SCAN_INTO_URL_GROUP.invoke(JSONObject.of("Url", weixinGroupLinkDetail.getContent()), multiValueMap);
            // 返回成功时或者失败时更新群链接状态
            log.info("返回值：{}", url);
        }
        return true;
    }
}
