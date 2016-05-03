package com.yuwnloy.disconman.annotations;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;

/**
 * Annotation used to indicate an EM Metric
 *
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricServiceTypeAnnotation {
  @DescriptorKey("DiagnosticTypeName")
  String value();
}
