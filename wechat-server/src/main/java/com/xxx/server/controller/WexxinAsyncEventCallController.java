package com.xxx.server.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.pojo.WeixinAsyncEventCall;
import com.xxx.server.service.IWeixinAsyncEventCallService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-08-06
 */
@RestController
@RequestMapping("/weixin-async-event-call")
@AllArgsConstructor
public class WexxinAsyncEventCallController {

    private IWeixinAsyncEventCallService weixinAsyncEventCallService;

    @GetMapping("queryAsyncEventCall")
    @ApiOperation("获取处理批次号")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "微信id", name = "wxId", paramType = "query"),
                    @ApiImplicitParam(value = "批次号类型:scanIntoUrlGroup 链接进群，groupChat：群聊， groupSend : 拉群", name = "eventType", paramType = "query", allowableValues = "scanIntoUrlGroup,groupChat,groupSend"),
            }
    )
    public String queryAsyncEventCall(String wxId, String eventType) {
        WeixinAsyncEventCall weixinAsyncEventCall = weixinAsyncEventCallService.getOne(
                Wrappers.lambdaQuery(WeixinAsyncEventCall.class)
                        .eq(WeixinAsyncEventCall::getWxId, wxId)
                        .eq(WeixinAsyncEventCall::getEventType, eventType)
                        // 获取正在处理的该微信数据
                        .eq(WeixinAsyncEventCall::getResultCode, "99"));
        // 如果存在计划完成时间小于当前时间，直接将状态置为500的系统异常
        if (weixinAsyncEventCall != null) {
            if (weixinAsyncEventCall.getPlanTime() == null)
                weixinAsyncEventCallService.updateById(weixinAsyncEventCall.setResultCode(500).setResult("系统异常,手动矫正信息"));
            return weixinAsyncEventCall.getAsyncEventCallId().toString();
        }
        return "";
    }

}
