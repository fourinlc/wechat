package com.xxx.server.annotation.dict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dict {

    /**
     * 字典类型
     *
     * @return
     */
     String dictGroup() default  "";

    /**
     * 返回属性名
     *
     * @return
     */
    String value() default "";
}
