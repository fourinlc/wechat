package com.xxx.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @PackageName:com.xxx.server.pojo
 * @ClassName:RespBean Description:通用返回对象
 * @author: lc
 * @date 2022/7/21 19:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespBean {
    private long code;
    private String message;
    private Object obj;

    public static RespBean sucess(String message){
        return new RespBean(200,message,null);
    }

    public static RespBean sucess(String message,Object obj){
        return new RespBean(200,message,obj);
    }

    public static RespBean error(String message){
        return new RespBean(500,message,null);
    }

    public static RespBean error(String message,Object obj){
        return new RespBean(500,message,obj);
    }
}
