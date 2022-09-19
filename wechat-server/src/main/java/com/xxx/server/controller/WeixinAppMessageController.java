package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinAppMessage;
import com.xxx.server.service.IWeixinAppMessageService;
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
 * @since 2022-09-05
 */
@RestController
@RequestMapping("/weixin-app-message")
@AllArgsConstructor
public class WeixinAppMessageController {

    private IWeixinAppMessageService weixinAppMessageService;

    // 新增修改链接
    @GetMapping("saveOrUpdate")
    @ApiOperation("链接新增修改")
    public RespBean saveOrUpdate(WeixinAppMessage weixinAppMessage){
        weixinAppMessage.setAction("view").setShowType("0").setType("5").setSoundType("0");
        return RespBean.sucess(weixinAppMessageService.saveOrUpdate(weixinAppMessage) ? "新增修改成功" : "新增修改失败", weixinAppMessage.getAppMessageId());
    }

}
