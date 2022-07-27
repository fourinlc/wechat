package com.xxx.server.controller;


import com.xxx.server.service.IWeixinTempalateService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class WeixinTempalateController {

    private IWeixinTempalateService weixinTempalateService;

    /*@GetMapping("test")
    public void test(String chatRoomName, String keyA, String keyB, String templateName, List<Long> fileIds){
        weixinTempalateService.chatHandler(chatRoomName, keyA, keyB, templateName, fileIds);
    }*/



}
