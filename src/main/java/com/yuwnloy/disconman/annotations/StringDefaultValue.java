package com.yuwnloy.disconman.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to set the default value of a mbean attribute of type String. 
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StringDefaultValue {
  String value();
  //tanh added for defaultvalue;
  int version() default 0;
}
