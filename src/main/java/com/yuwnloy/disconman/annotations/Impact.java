package com.yuwnloy.disconman.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the impact of an operation.
 * 
 * Accepted values are:
 * <ul>
 * 		<li>MBeanOperationInfo.ACTION
 * 		<li>MBeanOperationInfo.ACTION_INFO
 * 		<li>MBeanOperationInfo.INFO
 * 		<li>MBeanOperationInfo.UNKNOWN
 * </ul>
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Impact {
  int value();
}
