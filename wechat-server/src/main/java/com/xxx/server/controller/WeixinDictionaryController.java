package com.xxx.server.controller;


import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.service.IWeixinDictionaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @ApiOperation("按需查询字典列表")
    @GetMapping("query")
    public RespBean query(WeixinDictionary weixinDictionary){
        return RespBean.sucess("查询字典成功", weixinDictionaryService.query(weixinDictionary));
    }

    @ApiOperation("新增修改字典列表")
    @PostMapping("batchUpdate")
    public RespBean batchUpdate(@RequestBody List<WeixinDictionary> weixinDictionaries){
        if(weixinDictionaryService.saveOrUpdateBatch(weixinDictionaries)){
            return RespBean.sucess("修改字典列表成功");
        }
        return RespBean.error("修改字典失败");
    }

}
