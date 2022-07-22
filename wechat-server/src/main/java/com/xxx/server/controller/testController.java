package com.xxx.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @PackageName:com.xxx.server.controller
 * @ClassName:testController Description:
 * @author: lc
 * @date 2022/7/22 14:46
 */
@RestController
public class testController {
    @GetMapping("hello")
    public String hello(){
        return "hello";
    }
}
