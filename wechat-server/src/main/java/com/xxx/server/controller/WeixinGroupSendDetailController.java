package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.pojo.GroupSendParam;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinContactDetailedInfo;
import com.xxx.server.service.IWeixinGroupSendDetailService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-08-30
 */
@RestController
@RequestMapping("/weixin-group-send-detail")
@AllArgsConstructor
public class WeixinGroupSendDetailController {

    private IWeixinGroupSendDetailService weixinGroupSendDetailService;

    @PostMapping("batchGroupSendDetail")
    @ApiOperation("批量拉群")
    public JSONObject groupSendDetail(@RequestBody GroupSendParam groupSendParam){
        return weixinGroupSendDetailService.groupSendDetail(groupSendParam.getWeixinContactDetailedInfos(), groupSendParam.getMasterWxId(), groupSendParam.getSlaveWxIds(), groupSendParam.isFlag(), new Date());
    }

    @GetMapping("queryList")
    @ApiOperation("指定批次号获取拉群详情")
    public RespBean queryList(Long asyncEventCallId){
        return RespBean.sucess("获取拉群详情成功", weixinGroupSendDetailService.queryList(asyncEventCallId));
    }

}
