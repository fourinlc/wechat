package com.xxx.server.aop;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.server.annotation.dict.Dict;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.service.IWeixinDictionaryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 统一字典转换处理
 */
@Slf4j
@Component
@Aspect
@AllArgsConstructor
public class DictAspect {

    private IWeixinDictionaryService weixinDictionaryService;

    /**
     * 切点，切入 controller 包下面的所有方法,查询类型方法
     */
    @Pointcut("execution( * com.xxx.server.controller.*.*query(..))")
    public void dict() {

    }

    @Around("dict()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        long time1 = System.currentTimeMillis();
        Object result = pjp.proceed();
        long time2 = System.currentTimeMillis();
        log.debug("获取JSON数据 耗时：" + (time2 - time1) + "ms");
        long start = System.currentTimeMillis();
        this.parseDictText(result);
        long end = System.currentTimeMillis();
        log.debug("解析注入JSON数据 耗时" + (end - start) + "ms");
        return result;
    }

    private void parseDictText(Object result)  {
        if (result instanceof RespBean) {
            List<JSONObject> items = new ArrayList<>();
            RespBean respBean = (RespBean) result;
            if (respBean.getObj() != null && respBean.getObj() instanceof Collection) {
                List<?> list = (List<?>) respBean.getObj();
                for (Object record : list) {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = "{}";
                    try {
                        // 解决@JsonFormat注解解析不了的问题详见SysAnnouncement类的@JsonFormat
                        json = mapper.writeValueAsString(record);
                    } catch (JsonProcessingException e) {
                        log.error("Json解析失败：" + e);
                    }
                    JSONObject item = JSONObject.parseObject(json);
                    // 解决继承实体字段无法翻译问题
                    for (Field field : ReflectUtil.getFields(record.getClass())) {
                        //解决继承实体字段无法翻译问题
                        // 如果该属性上面有@Dict注解，则进行翻译
                        Dict annotation = field.getAnnotation(Dict.class);
                        if (annotation != null) {
                            // 拿到注解的dictDataSource属性的值,及对应的表名
                            String dicGroup = StrUtil.isEmpty(annotation.dictGroup()) ? SqlHelper.table(record.getClass()).getTableName(): annotation.dictGroup();
                            // 取字段名称为当前code
                            String dicCode = field.getName();
                            // 拿到注解的dictText属性的值
                            String text = annotation.value();
                            //获取当前带翻译的值
                            String key = String.valueOf(item.get(dicCode));
                            //翻译字典值对应的text值
                            String textValue = translateDictValue(dicGroup, dicCode, key);
                            // DICT_TEXT_SUFFIX的值为，是默认值：
                            // public static final String DICT_TEXT_SUFFIX = "_dictText";
                            //如果给了文本名
                            if (!StrUtil.isBlank(text)) {
                                item.put(text, textValue);
                            }else {
                                item.put(field.getName() , textValue);
                            }
                        }
                        // date类型默认转换string格式化日期
                        if ("java.util.Date".equals(field.getType().getName())
                                && field.getAnnotation(JsonFormat.class) == null
                                && item.get(field.getName()) != null) {
                            SimpleDateFormat aDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            item.put(field.getName(), aDate.format(new Date((Long) item.get(field.getName()))));
                        }
                    }
                    items.add(item);
                }
                respBean.setObj(items);
            }
        }
    }

    /**
     * 翻译字典文本
     *
     * @param dicGroup
     * @param key
     * @return
     */
    private String translateDictValue(String dicGroup, String dicCode, String key) {
        StringBuilder textValue = new StringBuilder();
        List<WeixinDictionary> weixinDictionaries = weixinDictionaryService.query(new WeixinDictionary().setDicCode(dicCode).setDicGroup(dicGroup).setDicKey(key));
        if (weixinDictionaries.size()> 0 && weixinDictionaries.get(0).getDicValue() != null) {
            if (!"".equals(textValue.toString())) {
                textValue.append(",");
            }
            textValue.append(weixinDictionaries.get(0).getDicValue());
        }
        return textValue.toString();
    }
}
