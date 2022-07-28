package com.xxx.server.controller;


import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.UpdateValid;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinTempalate;
import com.xxx.server.service.IWeixinTempalateService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
public class WeixinTempalateController {

    private IWeixinTempalateService weixinTempalateService;

    @GetMapping("add")
    @Validated(AddValid.class)
    public RespBean add(@Valid WeixinTempalate weixinTempalate){
        return weixinTempalateService.save(weixinTempalate) ? RespBean.sucess("新增成功") : RespBean.error("新增失败");
    }

    @GetMapping("update")
    @Validated(UpdateValid.class)
    public RespBean update(@Valid WeixinTempalate weixinTempalate){
        return weixinTempalateService.updateById(weixinTempalate) ? RespBean.sucess("修改成功") : RespBean.error("修改失败");
    }

    @GetMapping("query")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(value = "模板名称", name = "templateName", paramType = "query"),
            }
    )
    public RespBean query(String templateName){
        return RespBean.sucess("查询成功", weixinTempalateService.queryList(new WeixinTempalate().setTemplateName(templateName)));
    }

}
