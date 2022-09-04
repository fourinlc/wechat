package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.GroupChatNewParam;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

 /*   @ApiOperation("构建群发模板")
    @PostMapping("groupChat")
    public JSONObject groupChat(@RequestBody GroupChatParam groupChat){
        return weixinTemplateService.groupChat(groupChat.getChatRoomNames(), groupChat.getWxId(), groupChat.getTemplateIds(), groupChat.getFixedTime());
    }*/

    @ApiOperation("删除模板")
    @GetMapping("deleteByName")
    public RespBean deleteByName( String templateName){
        Assert.hasText(templateName, "模板名称不能为空");
        return RespBean.sucess(weixinTemplateService.deleteByName(templateName) ? "删除成功" : "删除失败");
    }

    @ApiOperation("模板群发消息")
    @PostMapping("groupChatNew")
    public JSONObject groupChatNew(@RequestBody GroupChatNewParam groupChatNew){
        return weixinTemplateService.groupChatNew(groupChatNew.getChatRoomNames(), groupChatNew.getWxIds(), groupChatNew.getTemplateIds(), groupChatNew.getFixedTime());
    }

  /*  @PostConstruct*/
    public void init(){
        String str = "<appmsg appid=\"\" sdkver=\"0\">\n" +
                "\t\t<title>ahasf</title>\n" +
                "\t\t<des>队长223123fgd</des>\n" +
                "\t\t<action>view</action>\n" +
                "\t\t<type>5</type>\n" +
                "\t\t<showtype>0</showtype>\n" +
                "\t\t<soundtype>0</soundtype>\n" +
                "\t\t<mediatagname />\n" +
                "\t\t<messageext />\n" +
                "\t\t<messageaction />\n" +
                "\t\t<content />\n" +
                "\t\t<contentattr>0</contentattr>\n" +
                "\t\t<url>https://baidu.com</url>\n" +
                "\t\t<lowurl />\n" +
                "\t\t<lowdataurl />\n" +
                "\t\t<extinfo />\n" +
                "\t\t<sourceusername />\n" +
                "\t\t<sourcedisplayname />\n" +
                "\t\t<thumburl />\n" +
                "\t</appmsg>\n" +
                "\t<commenturl />";
        System.out.println(str);
        JSONObject param = JSONObject.of("AppList", JSONArray.of(JSONObject.of("ToUserName", "wxid_tk8ml7phzo3y12", "ContentXML", str, "ContentType", 49)));
        MultiValueMap n = new LinkedMultiValueMap();
        n.add("key", "19a776d2-4608-4925-95c6-bc6d3fe7a112");
        System.out.println(WechatApiHelper.SEND_APP_MESSAGE.invoke(param, n));
    }

}
