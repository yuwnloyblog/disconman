package com.yuwnloy.disconman.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
/**
 * Specify if an attribute of operation is for internal use only.
 * Internal attributes and operations should only be used by dev and hidden (not documented)
 * to customers.
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hidden {
}
