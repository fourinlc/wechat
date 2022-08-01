package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.service.IWeixinDictionaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
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
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-dictionary")
@AllArgsConstructor
@Api(tags = "字典配置")
public class WeixinDictionaryController {

    private IWeixinDictionaryService weixinDictionaryService;

    // TODO 是否有必要限制不查询所有字典以及暴露清除接口
    @ApiOperation("按需查询字典列表")
    @GetMapping("query")
    public RespBean query(WeixinDictionary weixinDictionary){
        return RespBean.sucess("查询字典成功", weixinDictionaryService.query(weixinDictionary));
    }

}
