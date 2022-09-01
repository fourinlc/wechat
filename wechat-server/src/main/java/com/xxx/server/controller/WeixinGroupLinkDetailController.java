package com.xxx.server.controller;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-group-link-detail")
@AllArgsConstructor
@Api(tags = "群链接信息")
public class WeixinGroupLinkDetailController {

    private IWeixinGroupLinkDetailService weixinGroupLinkDetailService;

    @GetMapping("query")
    @ApiOperation("获取邀请链接")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "邀请时间", name = "invitationTime", paramType = "query"),
                    @ApiImplicitParam(value = "邀请人名称", name = "fromUserName", paramType = "query"),
                    @ApiImplicitParam(value = "邀请状态", name = "linkStatus", paramType = "query", allowMultiple = true),
                    @ApiImplicitParam(value = "被邀请人id", name = "toUserWxId", paramType = "query"),
                    @ApiImplicitParam(value = "有效标识", name = "invalidStatus", paramType = "query"),
                    @ApiImplicitParam(value = "验证群标识", name = "verifyStatus", paramType = "query"),
                    @ApiImplicitParam(value = "企业微信群标识", name = "companyStatus", paramType = "query"),
                    @ApiImplicitParam(value = "重复标识", name = "repeatStatus", paramType = "query")
            }
    )
    public RespBean query(@ApiIgnore WeixinGroupLinkDetail weixinGroupLinkDetail, @ApiIgnore @RequestParam("linkStatus") List<String> linkStatus) {
        return RespBean.sucess("查询成功", weixinGroupLinkDetailService.query(weixinGroupLinkDetail, linkStatus));
    }

    @GetMapping("batchScanIntoUrlGroup")
    @ApiOperation("批量进群")
    public JSONObject batchScanIntoUrlGroup(@RequestParam("linkIds") List<Long> linkIds, @RequestParam("wxIds") List<String> wxIds, String wxId){
        return weixinGroupLinkDetailService.batchScanIntoUrlGroup(linkIds, wxIds, wxId);
    }

}
