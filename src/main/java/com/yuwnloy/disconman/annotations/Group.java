package com.yuwnloy.disconman.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate group of a mbean interface to compose objectName.
 * 
 * @author xiaoguang.gao
 *
 * @date May 6, 2016
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Group {
	String value();
}
