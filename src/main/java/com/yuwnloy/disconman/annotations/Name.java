package com.yuwnloy.disconman.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate name of a parameter.  
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.TYPE})
public @interface Name {
  String value();
}
