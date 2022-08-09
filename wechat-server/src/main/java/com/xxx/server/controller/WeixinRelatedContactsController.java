package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.service.IWeixinRelatedContactsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-08-09
 */
@RestController
@RequestMapping("/weixin-related-contacts")
@AllArgsConstructor
@Slf4j
@Api(tags = "关联好友")
public class WeixinRelatedContactsController {
    IWeixinRelatedContactsService weixinRelatedContactsService;

    @ApiOperation(value = "关联好友")
    @PostMapping("/relatedFriends")
    public RespBean relatedFriends(String wxId, @RequestBody List<String> relatedWxIds){
        return weixinRelatedContactsService.relatedFriends(wxId, relatedWxIds);
    }

    @ApiOperation(value = "获取关联好友")
    @GetMapping("/getRelatedFriends")
    public RespBean getRelatedFriends(String wxId){
        return weixinRelatedContactsService.getRelatedFriends(wxId);
    }

    @ApiOperation(value = "取消关联好友")
    @PostMapping("/cancelRelatedFriends")
    public RespBean cancelRelatedFriends(String wxId, @RequestBody List<String> relatedWxIds){
        return weixinRelatedContactsService.cancelRelatedFriends(wxId, relatedWxIds);
    }
}
