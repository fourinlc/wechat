package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.service.IWeixinTemplateSendDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-08-16
 */
@RestController
@RequestMapping("/weixin-template-send-detail")
@AllArgsConstructor
@Api(tags = "群聊操作记录")
public class WeixinTemplateSendDetailController {

    private IWeixinTemplateSendDetailService weixinTemplateSendDetailService;

    @ApiOperation("获取子账号所有群信息")
    @GetMapping("/queryList")
    public RespBean queryList(String wxId, boolean refresh){
        return RespBean.sucess("获取信息成功", weixinTemplateSendDetailService.queryList(wxId, refresh));
    }

}
