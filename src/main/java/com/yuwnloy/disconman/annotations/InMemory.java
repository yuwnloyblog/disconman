package com.yuwnloy.disconman.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation to indicate that a value is only stored in memory 
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InMemory {
}
