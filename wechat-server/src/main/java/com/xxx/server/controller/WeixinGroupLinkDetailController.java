package com.xxx.server.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinGroupLinkDetail;
import com.xxx.server.pojo.WeixinTempalate;
import com.xxx.server.service.IWeixinGroupLinkDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public RespBean query(WeixinGroupLinkDetail weixinGroupLinkDetail) {
        return RespBean.sucess("获取邀请链接", weixinGroupLinkDetailService
                .list(Wrappers.<WeixinGroupLinkDetail>lambdaQuery()
                        .eq(StrUtil.isNotEmpty(weixinGroupLinkDetail.getInvitationTime()), WeixinGroupLinkDetail::getInvitationTime, weixinGroupLinkDetail.getInvitationTime())
                        .like(StrUtil.isNotEmpty(weixinGroupLinkDetail.getFromUserName()), WeixinGroupLinkDetail::getFromUserName, weixinGroupLinkDetail.getFromUserName())));
    }

}
