package com.yuwnloy.disconman.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate domain of a mbean interface to compose objectName.
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Domain {
  String value();
}

