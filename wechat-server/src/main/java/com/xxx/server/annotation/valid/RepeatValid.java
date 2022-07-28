package com.xxx.server.annotation.valid;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @ProjectName: RuoYi
 * @Package: com.kedong.project.kedong.valid
 * @ClassName: Repeat
 * @Author: quj
 * @Description: ${description}
 * @Date: 2021/12/10 16:24
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = RepeatValidator.class)
@Repeatable(RepeatValid.List.class)
public @interface RepeatValid {

    String message();

    String fieldName();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {

        RepeatValid[] value();
    }

}
