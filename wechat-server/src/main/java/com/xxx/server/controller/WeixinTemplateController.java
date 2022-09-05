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

import javax.annotation.PostConstruct;

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
        return weixinTemplateService.groupChatNew(groupChatNew.getWeixinContactDetailedInfos(), groupChatNew.getWxIds(), groupChatNew.getTemplateIds(), groupChatNew.getFixedTime());
    }

    /*@PostConstruct*/
    public void init(){
        String str = "<appmsg appid=\"\" sdkver=\"0\">\n" +
                "\t\t<title>wechat</title>\n" +
                "\t\t<des>快乐的早上</des>\n" +
                "\t\t<action>view</action>\n" +
                "\t\t<type>5</type>\n" +
                "\t\t<showtype>0</showtype>\n" +
                "\t\t<soundtype>0</soundtype>\t\n" +
                "\t\t<contentattr>0</contentattr>\n" +
                "\t\t<url>https://baidu.com</url>\n" +
                "\t\t<thumburl><![CDATA[http://120.77.218.220/wechat/images/3/9d4096b8fc2a0f97f95fef5e65284ca2.jpeg]]></thumburl>\n" +
                "\t</appmsg>";
        JSONObject param = JSONObject.of("AppList", JSONArray.of(JSONObject.of("ToUserName", "19976248534@chatroom", "ContentXML", str, "ContentType", 49)));
        MultiValueMap n = new LinkedMultiValueMap();
        n.add("key", "19a776d2-4608-4925-95c6-bc6d3fe7a112");
        System.out.println(WechatApiHelper.SEND_APP_MESSAGE.invoke(param, n));
    }

    public static void main(String[] args) {
        String str = "E632959801:\n\u003c?xml version=\"1.0\"?\u003e\n\u003cmsg\u003e\n\t\u003cappmsg appid=\"\" sdkver=\"0\"\u003e\n\t\t\u003ctitle\u003e轻松筹 | 女孩患【罕见病】，倾见荡产移植，把我器官捐给有用的人。\u003c/title\u003e\n\t\t\u003cdes\u003e妈妈：我不治了，要那么多钱，我们家治不起，我不想拖累你们，带我回家吧，把钱留给弟弟妹妹上学，听到孩子说出的话我的心在滴血.......“再生障碍性贫血”这个只是在电视上看到的名字,竟然活生生发生在我家\u003c/des\u003e\n\t\t\u003cusername /\u003e\n\t\t\u003caction\u003eview\u003c/action\u003e\n\t\t\u003ctype\u003e5\u003c/type\u003e\n\t\t\u003cshowtype\u003e0\u003c/showtype\u003e\n\t\t\u003ccontent /\u003e\n\t\t\u003curl\u003ehttps://m2.qschou.com/project/detail/sharemiddle/H5XyMdbD3yFywPTmRDmX4Q284J5r8fKE.html?middletz=https%3A%2F%2Fm2.qschou.com%2Ffund%2Fdetail%3Fprojuuid%3Dc412aebc-e247-4909-b6c2-94140a3b5fec%26shareuuid%3D58b05b1f-7fbb-11e9-8289-00163e0cc11b%26share_no%3D18303e642d721e-0f091e5d60ebda-74512f1f-56d10-18303e642d91e9%26level%3D1%26sharecc%3D80X36.v8_1%26shareto%3D2%26sharecount%3D1%26platform%3Dwechat%26timestamp%3D2022090323092945851581957%26godeviceid%3D182f81a5290329-0612a30b2dbb27-74512f6e-56d10-182f81a529130d%26mp%3Drocket.8.wx99e975198fd721c5.0_2_3.142075\u003c/url\u003e\n\t\t\u003clowurl /\u003e\n\t\t\u003cforwardflag\u003e0\u003c/forwardflag\u003e\n\t\t\u003cdataurl /\u003e\n\t\t\u003clowdataurl /\u003e\n\t\t\u003ccontentattr\u003e0\u003c/contentattr\u003e\n\t\t\u003cstreamvideo\u003e\n\t\t\t\u003cstreamvideourl /\u003e\n\t\t\t\u003cstreamvideototaltime\u003e0\u003c/streamvideototaltime\u003e\n\t\t\t\u003cstreamvideotitle /\u003e\n\t\t\t\u003cstreamvideowording /\u003e\n\t\t\t\u003cstreamvideoweburl /\u003e\n\t\t\t\u003cstreamvideothumburl /\u003e\n\t\t\t\u003cstreamvideoaduxinfo /\u003e\n\t\t\t\u003cstreamvideopublishid /\u003e\n\t\t\u003c/streamvideo\u003e\n\t\t\u003ccanvasPageItem\u003e\n\t\t\t\u003ccanvasPageXml\u003e\u003c![CDATA[]]\u003e\u003c/canvasPageXml\u003e\n\t\t\u003c/canvasPageItem\u003e\n\t\t\u003cappattach\u003e\n\t\t\t\u003cattachid /\u003e\n\t\t\t\u003ccdnthumburl\u003e3057020100044b3049020100020406f35fb002032f501d02046eb41575020463152dac042431646339313862372d353235342d343734372d383939612d3535626634353363333661390204011400030201000405004c53d900\u003c/cdnthumburl\u003e\n\t\t\t\u003ccdnthumbmd5\u003e0bee4132db9163d391b91f7c8a2f9f1f\u003c/cdnthumbmd5\u003e\n\t\t\t\u003ccdnthumblength\u003e34111\u003c/cdnthumblength\u003e\n\t\t\t\u003ccdnthumbheight\u003e120\u003c/cdnthumbheight\u003e\n\t\t\t\u003ccdnthumbwidth\u003e120\u003c/cdnthumbwidth\u003e\n\t\t\t\u003ccdnthumbaeskey\u003e79857130c87e53ed8a4f2aeb561eb4c8\u003c/cdnthumbaeskey\u003e\n\t\t\t\u003caeskey\u003e79857130c87e53ed8a4f2aeb561eb4c8\u003c/aeskey\u003e\n\t\t\t\u003cencryver\u003e1\u003c/encryver\u003e\n\t\t\t\u003cfileext /\u003e\n\t\t\t\u003cislargefilemsg\u003e0\u003c/islargefilemsg\u003e\n\t\t\u003c/appattach\u003e\n\t\t\u003cextinfo /\u003e\n\t\t\u003candroidsource\u003e3\u003c/androidsource\u003e\n\t\t\u003csourceusername\u003egh_2b175d6b42be\u003c/sourceusername\u003e\n\t\t\u003csourcedisplayname /\u003e\n\t\t\u003ccommenturl /\u003e\n\t\t\u003cthumburl /\u003e\n\t\t\u003cmediatagname /\u003e\n\t\t\u003cmessageaction\u003e\u003c![CDATA[]]\u003e\u003c/messageaction\u003e\n\t\t\u003cmessageext\u003e\u003c![CDATA[]]\u003e\u003c/messageext\u003e\n\t\t\u003cemoticongift\u003e\n\t\t\t\u003cpackageflag\u003e0\u003c/packageflag\u003e\n\t\t\t\u003cpackageid /\u003e\n\t\t\u003c/emoticongift\u003e\n\t\t\u003cemoticonshared\u003e\n\t\t\t\u003cpackageflag\u003e0\u003c/packageflag\u003e\n\t\t\t\u003cpackageid /\u003e\n\t\t\u003c/emoticonshared\u003e\n\t\t\u003cdesignershared\u003e\n\t\t\t\u003cdesigneruin\u003e0\u003c/designeruin\u003e\n\t\t\t\u003cdesignername\u003enull\u003c/designername\u003e\n\t\t\t\u003cdesignerrediretcturl\u003enull\u003c/designerrediretcturl\u003e\n\t\t\u003c/designershared\u003e\n\t\t\u003cemotionpageshared\u003e\n\t\t\t\u003ctid\u003e0\u003c/tid\u003e\n\t\t\t\u003ctitle\u003enull\u003c/title\u003e\n\t\t\t\u003cdesc\u003enull\u003c/desc\u003e\n\t\t\t\u003ciconUrl\u003enull\u003c/iconUrl\u003e\n\t\t\t\u003csecondUrl /\u003e\n\t\t\t\u003cpageType\u003e0\u003c/pageType\u003e\n\t\t\u003c/emotionpageshared\u003e\n\t\t\u003cwebviewshared\u003e\n\t\t\t\u003cshareUrlOriginal /\u003e\n\t\t\t\u003cshareUrlOpen /\u003e\n\t\t\t\u003cjsAppId /\u003e\n\t\t\t\u003cpublisherId /\u003e\n\t\t\u003c/webviewshared\u003e\n\t\t\u003ctemplate_id /\u003e\n\t\t\u003cmd5\u003e0bee4132db9163d391b91f7c8a2f9f1f\u003c/md5\u003e\n\t\t\u003cweappinfo\u003e\n\t\t\t\u003cusername /\u003e\n\t\t\t\u003cappid /\u003e\n\t\t\t\u003cappservicetype\u003e0\u003c/appservicetype\u003e\n\t\t\t\u003csecflagforsinglepagemode\u003e0\u003c/secflagforsinglepagemode\u003e\n\t\t\t\u003cvideopageinfo\u003e\n\t\t\t\t\u003cthumbwidth\u003e120\u003c/thumbwidth\u003e\n\t\t\t\t\u003cthumbheight\u003e120\u003c/thumbheight\u003e\n\t\t\t\t\u003cfromopensdk\u003e0\u003c/fromopensdk\u003e\n\t\t\t\u003c/videopageinfo\u003e\n\t\t\u003c/weappinfo\u003e\n\t\t\u003cstatextstr /\u003e\n\t\t\u003cmusicShareItem\u003e\n\t\t\t\u003cmusicDuration\u003e0\u003c/musicDuration\u003e\n\t\t\u003c/musicShareItem\u003e\n\t\t\u003cfinderLiveProductShare\u003e\n\t\t\t\u003cfinderLiveID\u003e\u003c![CDATA[]]\u003e\u003c/finderLiveID\u003e\n\t\t\t\u003cfinderUsername\u003e\u003c![CDATA[]]\u003e\u003c/finderUsername\u003e\n\t\t\t\u003cfinderObjectID\u003e\u003c![CDATA[]]\u003e\u003c/finderObjectID\u003e\n\t\t\t\u003cfinderNonceID\u003e\u003c![CDATA[]]\u003e\u003c/finderNonceID\u003e\n\t\t\t\u003cliveStatus\u003e\u003c![CDATA[]]\u003e\u003c/liveStatus\u003e\n\t\t\t\u003cappId\u003e\u003c![CDATA[]]\u003e\u003c/appId\u003e\n\t\t\t\u003cpagePath\u003e\u003c![CDATA[]]\u003e\u003c/pagePath\u003e\n\t\t\t\u003cproductId\u003e\u003c![CDATA[]]\u003e\u003c/productId\u003e\n\t\t\t\u003ccoverUrl\u003e\u003c![CDATA[]]\u003e\u003c/coverUrl\u003e\n\t\t\t\u003cproductTitle\u003e\u003c![CDATA[]]\u003e\u003c/productTitle\u003e\n\t\t\t\u003cmarketPrice\u003e\u003c![CDATA[0]]\u003e\u003c/marketPrice\u003e\n\t\t\t\u003csellingPrice\u003e\u003c![CDATA[0]]\u003e\u003c/sellingPrice\u003e\n\t\t\t\u003cplatformHeadImg\u003e\u003c![CDATA[]]\u003e\u003c/platformHeadImg\u003e\n\t\t\t\u003cplatformName\u003e\u003c![CDATA[]]\u003e\u003c/platformName\u003e\n\t\t\t\u003cshopWindowId\u003e\u003c![CDATA[]]\u003e\u003c/shopWindowId\u003e\n\t\t\t\u003cflashSalePrice\u003e\u003c![CDATA[0]]\u003e\u003c/flashSalePrice\u003e\n\t\t\t\u003cflashSaleEndTime\u003e\u003c![CDATA[0]]\u003e\u003c/flashSaleEndTime\u003e\n\t\t\t\u003cecSource\u003e\u003c![CDATA[]]\u003e\u003c/ecSource\u003e\n\t\t\t\u003csellingPriceWording\u003e\u003c![CDATA[]]\u003e\u003c/sellingPriceWording\u003e\n\t\t\u003c/finderLiveProductShare\u003e\n\t\t\u003cfinderShopWindowShare\u003e\n\t\t\t\u003cfinderUsername\u003e\u003c![CDATA[]]\u003e\u003c/finderUsername\u003e\n\t\t\t\u003cavatar\u003e\u003c![CDATA[]]\u003e\u003c/avatar\u003e\n\t\t\t\u003cnickname\u003e\u003c![CDATA[]]\u003e\u003c/nickname\u003e\n\t\t\t\u003ccommodityInStockCount\u003e\u003c![CDATA[]]\u003e\u003c/commodityInStockCount\u003e\n\t\t\t\u003cappId\u003e\u003c![CDATA[]]\u003e\u003c/appId\u003e\n\t\t\t\u003cpath\u003e\u003c![CDATA[]]\u003e\u003c/path\u003e\n\t\t\t\u003cappUsername\u003e\u003c![CDATA[]]\u003e\u003c/appUsername\u003e\n\t\t\t\u003cquery\u003e\u003c![CDATA[]]\u003e\u003c/query\u003e\n\t\t\t\u003cliteAppId\u003e\u003c![CDATA[]]\u003e\u003c/liteAppId\u003e\n\t\t\t\u003cliteAppPath\u003e\u003c![CDATA[]]\u003e\u003c/liteAppPath\u003e\n\t\t\t\u003cliteAppQuery\u003e\u003c![CDATA[]]\u003e\u003c/liteAppQuery\u003e\n\t\t\u003c/finderShopWindowShare\u003e\n\t\t\u003cfindernamecard\u003e\n\t\t\t\u003cusername /\u003e\n\t\t\t\u003cavatar\u003e\u003c![CDATA[]]\u003e\u003c/avatar\u003e\n\t\t\t\u003cnickname /\u003e\n\t\t\t\u003cauth_job /\u003e\n\t\t\t\u003cauth_icon\u003e0\u003c/auth_icon\u003e\n\t\t\t\u003cauth_icon_url /\u003e\n\t\t\u003c/findernamecard\u003e\n\t\t\u003cfinderGuarantee\u003e\n\t\t\t\u003cscene\u003e\u003c![CDATA[0]]\u003e\u003c/scene\u003e\n\t\t\u003c/finderGuarantee\u003e\n\t\t\u003cdirectshare\u003e0\u003c/directshare\u003e\n\t\t\u003cgamecenter\u003e\n\t\t\t\u003cnamecard\u003e\n\t\t\t\t\u003ciconUrl /\u003e\n\t\t\t\t\u003cname /\u003e\n\t\t\t\t\u003cdesc /\u003e\n\t\t\t\t\u003ctail /\u003e\n\t\t\t\t\u003cjumpUrl /\u003e\n\t\t\t\u003c/namecard\u003e\n\t\t\u003c/gamecenter\u003e\n\t\t\u003cpatMsg\u003e\n\t\t\t\u003cchatUser /\u003e\n\t\t\t\u003crecords\u003e\n\t\t\t\t\u003crecordNum\u003e0\u003c/recordNum\u003e\n\t\t\t\u003c/records\u003e\n\t\t\u003c/patMsg\u003e\n\t\t\u003csecretmsg\u003e\n\t\t\t\u003cissecretmsg\u003e0\u003c/issecretmsg\u003e\n\t\t\u003c/secretmsg\u003e\n\t\t\u003creferfromscene\u003e0\u003c/referfromscene\u003e\n\t\t\u003cwebsearch /\u003e\n\t\u003c/appmsg\u003e\n\t\u003cfromusername\u003eE632959801\u003c/fromusername\u003e\n\t\u003cscene\u003e0\u003c/scene\u003e\n\t\u003cappinfo\u003e\n\t\t\u003cversion\u003e1\u003c/version\u003e\n\t\t\u003cappname /\u003e\n\t\u003c/appinfo\u003e\n\t\u003ccommenturl /\u003e\n\u003c/msg\u003e\n";
        System.out.println(str);
    }

}
