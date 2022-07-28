package com.xxx.server.annotation.valid;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: RepeatValidator
 * @Description: 校验字段的是否再数据库唯一性
 */
@Component
public class RepeatValidator implements ConstraintValidator<RepeatValid, Object> {

    private String message;

    private String fieldName;

    private Class<?>[] groups;

    @Resource
    private Map<String, BaseMapper> baseMappers;

    @SneakyThrows
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 暂时只类上边注解处理
        String tableName = value.getClass().getSimpleName();
        BaseMapper baseMapper = baseMappers.get(StrUtil.lowerFirst(tableName) + "Mapper");
        // 获取操作的基础类
        for (Class<?> group : groups) {
            if (group == UpdateValid.class) {
                // 原始该条数据对应的数据
                Object o = baseMapper.selectById((Serializable) value);
                // 比较两个待校验的数据是否有变化,相同数据时
                if (ObjectUtil.equal(ReflectUtil.getFieldValue(o, fieldName),ReflectUtil.getFieldValue(value, fieldName))) {
                    return true;
                }
                // 查询该结果集是否包含本数据源
                if (checkRepeat(value, baseMapper)) {
                    return false;
                }
                // 比较两个待校验的数据是否有变化
            }else if(group == AddValid.class){
                // 处理新增操作校验
                if (checkRepeat(value, baseMapper)) {
                    return false;
                }
            }/*else {
                // ignore
            }*/
        }
        return true;
    }

    private boolean checkRepeat(Object value, BaseMapper mapper) throws InstantiationException, IllegalAccessException {
        Object o1 = value.getClass().newInstance();
        ReflectUtil.setFieldValue(o1, fieldName, ReflectUtil.getFieldValue(value, fieldName));
        List select = mapper.selectList(Wrappers.query(o1));
        if (select.size() > 0) {
            // 重复数据,校验失败
            return true;
        }
        return false;
    }

    // 获取注解上所需要的值
    @Override
    public void initialize(RepeatValid constraintAnnotation) {
        this.groups = constraintAnnotation.groups();
        this.message = constraintAnnotation.message();
        this.fieldName = constraintAnnotation.fieldName();
    }
}
