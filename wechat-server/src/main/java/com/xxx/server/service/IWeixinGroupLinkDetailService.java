package com.xxx.server.service;

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

    boolean batchScanIntoUrlGroup(List<Long> linkIds);

}
