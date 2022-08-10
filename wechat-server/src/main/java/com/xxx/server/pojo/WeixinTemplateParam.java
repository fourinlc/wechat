package com.xxx.server.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WeixinTemplateParam implements Serializable {

    private WeixinTemplate weixinTemplate;

    private List<WeixinTemplateDetail> weixinTemplateDetailList;
}
