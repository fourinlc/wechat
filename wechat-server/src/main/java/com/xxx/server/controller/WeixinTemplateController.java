package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.pojo.WeixinTemplateParam;
import com.xxx.server.service.IWeixinTemplateDetailService;
import com.xxx.server.service.IWeixinTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
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

    private IWeixinTemplateDetailService weixinTemplateDetailService;

    @ApiOperation("新增修改")
    @PostMapping("addOrUpdate")
    public RespBean add(@RequestBody WeixinTemplateParam weixinTemplateParam){
        WeixinTemplate weixinTemplate = weixinTemplateParam.getWeixinTemplate();
        if (weixinTemplateService.getById(weixinTemplate.getTemplateId()) != null) {
            return RespBean.sucess(weixinTemplateService.update(weixinTemplate, weixinTemplateParam.getWeixinTemplateDetailList()) ? "修改成功" : "修改失败");
        }else {
            return RespBean.sucess(weixinTemplateService.add(weixinTemplate, weixinTemplateParam.getWeixinTemplateDetailList()) ? "新增成功" : "新增失败");
        }
    }

   /* @ApiOperation("查询")
    @GetMapping("query")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "模板名称", name = "templateName", paramType = "query"),
                    @ApiImplicitParam(value = "模板类型", name = "templateType", paramType = "query"),
            }
    )*/
    /*public RespBean query(String templateName, String templateType){
        return RespBean.sucess("查询成功", weixinTempalateService.queryList(new WeixinTemplate().setTemplateName(templateName).setTemplateType(templateType)));
    }*/

    /*@GetMapping("chatHandler")
    public RespBean chatHandler() throws InterruptedException {
        weixinTempalateService.chatHandler(Lists.newArrayList("开心", "快乐"), "广A", "广B", "test", null);
        return RespBean.sucess("查询成功");
    }*/

    @ApiOperation("删除模板")
    @GetMapping("deleteByName")
    public RespBean deleteByName( String templateName){
        return RespBean.sucess(weixinTemplateService.removeByMap(JSONObject.of("template_name", templateName)) ? "删除成功" : "删除失败");
    }

}
