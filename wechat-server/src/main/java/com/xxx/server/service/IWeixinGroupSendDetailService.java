package com.xxx.server.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.pojo.WeixinGroupSendDetail;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lc
 * @since 2022-08-29
 */
public interface IWeixinGroupSendDetailService extends IService<WeixinGroupSendDetail> {

    JSONObject groupSendDetail(List<WeixinContactDetailedInfo> weixinContactDetailedInfos, String masterWxId, List<String> slaveWxIds, boolean flag, Date fixedTime);

    JSONArray queryList(Long asyncEventCallId);

    JSONArray queryList(String createTime);

}
