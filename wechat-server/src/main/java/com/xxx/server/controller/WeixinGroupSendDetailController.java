package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.service.IWeixinGroupSendDetailService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("batchGroupSendDetail")
    @ApiOperation("批量拉群")
    public JSONObject groupSendDetail(@RequestParam("chatRoomIds") List<String> chatRoomIds, String masterWxId, @RequestParam("slaveWxIds") List<String> slaveWxIds, boolean flag/*, Date fixedTime*/){
        return weixinGroupSendDetailService.groupSendDetail(chatRoomIds, masterWxId, slaveWxIds, flag, new Date());
    }

}
