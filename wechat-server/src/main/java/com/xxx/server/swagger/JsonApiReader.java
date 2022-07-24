package com.xxx.server.swagger;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import lombok.AllArgsConstructor;
import org.apache.ibatis.javassist.*;
import org.apache.ibatis.javassist.bytecode.AnnotationsAttribute;
import org.apache.ibatis.javassist.bytecode.ConstPool;
import org.apache.ibatis.javassist.bytecode.annotation.Annotation;
import org.apache.ibatis.javassist.bytecode.annotation.IntegerMemberValue;
import org.apache.ibatis.javassist.bytecode.annotation.StringMemberValue;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.util.Map;

@Component
@Order   //plugin加载顺序，默认是最后加载
@AllArgsConstructor
public class JsonApiReader implements ParameterBuilderPlugin {

    private TypeResolver typeResolver;
    //动态生成的Class名
    private final static String basePackage = "com.xxx.server.swagger.model.";

    @Override
    public void apply(ParameterContext parameterContext) {
        ResolvedMethodParameter methodParameter = parameterContext.resolvedMethodParameter();
        //判断是否需要修改对象ModelRef,这里我判断的是Map类型和String类型需要重新修改ModelRef对象
        if (methodParameter.getParameterType().canCreateSubtype(Map.class) || methodParameter.getParameterType().canCreateSubtype(String.class) || methodParameter.getParameterType().canCreateSubtype(JSONObject.class)) {
            Optional<ApiJsonObject> optional = methodParameter.findAnnotation(ApiJsonObject.class);  //根据参数上的ApiJsonObject注解中的参数动态生成Class
            if (optional.isPresent()) {
                String name = optional.get().name();  //model 名称
                ApiJsonProperty[] properties = optional.get().value();
                parameterContext.getDocumentationContext().getAdditionalModels().add(typeResolver.resolve(createRefModel(properties, name)));  //像documentContext的Models中添加我们新生成的Class
                //修改Map参数的ModelRef为我们动态生成的class
                parameterContext.parameterBuilder()
                        .parameterType("body")
                        .modelRef(new ModelRef(name))
                        .name(name);
            }
        }

    }

    /**
     * 根据propertys中的值动态生成含有Swagger注解的javaBeen
     */
    private Class createRefModel(ApiJsonProperty[] propertys, String name) {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(basePackage + name);
        try {
            for (ApiJsonProperty property : propertys) {
                ctClass.addField(createField(property, ctClass));
            }
            return ctClass.toClass();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据property的值生成含有swagger apiModelProperty注解的属性
     */
    private CtField createField(ApiJsonProperty property, CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtField ctField = new CtField(getFieldType(property.type()), property.key(), ctClass);
        ctField.setModifiers(Modifier.PUBLIC);

        ConstPool constPool = ctClass.getClassFile().getConstPool();

        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation ann = new Annotation("io.swagger.annotations.ApiModelProperty", constPool);
        ann.addMemberValue("value", new StringMemberValue(property.description(), constPool));
        if (ctField.getType().subclassOf(ClassPool.getDefault().get(String.class.getName())))
            ann.addMemberValue("example", new StringMemberValue(property.example(), constPool));
        if (ctField.getType().subclassOf(ClassPool.getDefault().get(Integer.class.getName())))
            ann.addMemberValue("example", new IntegerMemberValue(Integer.parseInt(property.example()), constPool));
        attr.addAnnotation(ann);
        ctField.getFieldInfo().addAttribute(attr);

        return ctField;
    }

    private CtClass getFieldType(String type) throws NotFoundException {
        CtClass fileType = null;
        switch (type) {
            case "string":
                fileType = ClassPool.getDefault().get(String.class.getName());
                break;
            case "int":
                fileType = ClassPool.getDefault().get(Integer.class.getName());
                break;
        }
        return fileType;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}