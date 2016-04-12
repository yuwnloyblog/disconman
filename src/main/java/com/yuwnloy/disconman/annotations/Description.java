package com.yuwnloy.disconman.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to set the description of a mbean attribute or method
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface Description {
  String value();
}
