package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.pojo.GroupChatParam;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.pojo.WeixinTemplateParam;
import com.xxx.server.service.IWeixinTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  AB话术控制类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-tempalate")
@AllArgsConstructor
@Api(tags = "模板操作")
@Validated
public class WeixinTemplateController {

    private IWeixinTemplateService weixinTemplateService;

    @ApiOperation("批量新增修改")
    @PostMapping("addOrUpdate")
    public RespBean add(@RequestBody WeixinTemplateParam weixinTemplateParam){
        WeixinTemplate weixinTemplate = weixinTemplateParam.getWeixinTemplate();
        if (weixinTemplateService.getById(weixinTemplate.getTemplateId()) != null) {
            return RespBean.sucess(weixinTemplateService.update(weixinTemplate, weixinTemplateParam.getWeixinTemplateDetailList()) ? "修改成功" : "修改失败");
        }else {
            return RespBean.sucess(weixinTemplateService.add(weixinTemplate, weixinTemplateParam.getWeixinTemplateDetailList()) ? "新增成功" : "新增失败");
        }
    }

    @ApiOperation("查询")
    @GetMapping("query")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "模板名称", name = "templateName", paramType = "query"),
                    @ApiImplicitParam(value = "模板类型", name = "templateType", paramType = "query"),
            }
    )
    public RespBean query(String templateName, String templateType){
        return RespBean.sucess("查询成功", weixinTemplateService.queryList(new WeixinTemplate().setTemplateName(templateName).setTemplateType(templateType)));
    }

    @ApiOperation("构建群发模板")
    @PostMapping("groupChat")
    public JSONObject groupChat(@RequestBody GroupChatParam groupChat){
        return weixinTemplateService.groupChat(groupChat.getChatRoomNames(), groupChat.getWxId(), groupChat.getTemplateIds(), groupChat.getFixedTime());
    }

    @ApiOperation("删除模板")
    @GetMapping("deleteByName")
    public RespBean deleteByName( String templateName){
        Assert.hasText(templateName, "模板名称不能为空");
        return RespBean.sucess(weixinTemplateService.deleteByName(templateName) ? "删除成功" : "删除失败");
    }

}
