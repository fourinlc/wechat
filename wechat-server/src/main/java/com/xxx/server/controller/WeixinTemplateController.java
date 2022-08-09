package com.xxx.server.controller;


import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.UpdateValid;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinTemplate;
import com.xxx.server.service.IWeixinTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.assertj.core.util.Lists;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
@Validated
@Api(tags = "模板操作")
public class WeixinTemplateController {

    private IWeixinTemplateService weixinTempalateService;

    @ApiOperation("新增")
    @PostMapping("add")
    @Validated(AddValid.class)
    public RespBean add(@Valid WeixinTemplate weixinTempalate){
        return weixinTempalateService.save(weixinTempalate) ? RespBean.sucess("新增成功") : RespBean.error("新增失败");
    }

    @ApiOperation("批量新增")
    @PostMapping("batchAdd")
    @Validated(AddValid.class)
    public RespBean batchAdd(@Valid @RequestBody List<WeixinTemplate> weixinTempalates){
        return weixinTempalateService.saveBatch(weixinTempalates) ? RespBean.sucess("批量新增成功") : RespBean.error("新增失败");
    }

    @ApiOperation("更新")
    @PostMapping("update")
    @Validated(UpdateValid.class)
    public RespBean update(@Valid WeixinTemplate weixinTempalate){
        return weixinTempalateService.updateById(weixinTempalate) ? RespBean.sucess("修改成功") : RespBean.error("修改失败");
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
        return RespBean.sucess("查询成功", weixinTempalateService.queryList(new WeixinTemplate().setTemplateName(templateName).setTemplateType(templateType)));
    }

    @GetMapping("chatHandler")
    public RespBean chatHandler() throws InterruptedException {
        weixinTempalateService.chatHandler(Lists.newArrayList("开心", "快乐"), "广A", "广B", "test", null);
        return RespBean.sucess("查询成功");
    }

}
