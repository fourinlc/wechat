package com.xxx.server.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinGroupLinkDetail;

import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
public interface IWeixinGroupLinkDetailService extends IService<WeixinGroupLinkDetail> {

    JSONObject batchScanIntoUrlGroup(List<Long> linkIds, List<String> wxIds, String wxId);

    /**批量保存邀请链接，并处理对应链接状态*/
    boolean saveBatch(List<WeixinGroupLinkDetail> weixinGroupLinkDetails);

    Object query(WeixinGroupLinkDetail weixinGroupLinkDetail, List<String> linkStatus);

    /*boolean batchScanIntoUrlGroupNew(List<Long> linkIds, List<String> wxIds, String fixedTime);*/
}
