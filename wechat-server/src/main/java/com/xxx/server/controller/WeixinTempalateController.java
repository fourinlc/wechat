package com.xxx.server.controller;


import com.xxx.server.annotation.valid.AddValid;
import com.xxx.server.annotation.valid.UpdateValid;
import com.xxx.server.pojo.WeixinTempalate;
import com.xxx.server.service.IWeixinTempalateService;
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

    /*@GetMapping("test")
    public void test(String chatRoomName, String keyA, String keyB, String templateName, List<Long> fileIds){
        weixinTempalateService.chatHandler(chatRoomName, keyA, keyB, templateName, fileIds);
    }*/

    @GetMapping("add")
    @Validated(AddValid.class)
    public void add(@Valid WeixinTempalate weixinTempalate){
        weixinTempalateService.save(weixinTempalate);
    }

    @GetMapping("update")
    @Validated(UpdateValid.class)
    public void update(@Valid WeixinTempalate weixinTempalate){
        weixinTempalateService.updateById(weixinTempalate);
    }

}
