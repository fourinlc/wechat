package com.xxx.server.controller;


import cn.hutool.core.util.StrUtil;
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
                    @ApiImplicitParam(value = "邀请状态", name = "linkStatus", paramType = "query"),
                    @ApiImplicitParam(value = "被邀请人id", name = "toUserWxId", paramType = "query"),
                    @ApiImplicitParam(value = "有效标识", name = "invalidStatus", paramType = "query"),
                    @ApiImplicitParam(value = "验证群标识", name = "verifyStatus", paramType = "query")
            }
    )
    public RespBean query(@ApiIgnore WeixinGroupLinkDetail weixinGroupLinkDetail) {
        return RespBean.sucess("获取邀请链接", weixinGroupLinkDetailService
                .list(Wrappers.<WeixinGroupLinkDetail>lambdaQuery()
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvitationTime()), WeixinGroupLinkDetail::getInvitationTime, weixinGroupLinkDetail.getInvitationTime())
                        .like(StrUtil.isNotEmpty(weixinGroupLinkDetail.getFromUserName()), WeixinGroupLinkDetail::getFromUserName, weixinGroupLinkDetail.getFromUserName())
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getLinkStatus()), WeixinGroupLinkDetail::getLinkStatus, weixinGroupLinkDetail.getLinkStatus())
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getToUserWxId()), WeixinGroupLinkDetail::getToUserWxId, weixinGroupLinkDetail.getToUserWxId())
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvalidStatus()), WeixinGroupLinkDetail::getInvalidStatus, weixinGroupLinkDetail.getInvalidStatus())
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getVerifyStatus()), WeixinGroupLinkDetail::getVerifyStatus, weixinGroupLinkDetail.getVerifyStatus())));
    }

    @GetMapping("batchScanIntoUrlGroup")
    @ApiOperation("批量进群")
    public RespBean batchScanIntoUrlGroup(@RequestParam(value = "linkIds") List<Long> linkIds){
        return RespBean.sucess(weixinGroupLinkDetailService.batchScanIntoUrlGroup(linkIds) ? "成功" : "该群链接已被处理过");
    }

}
