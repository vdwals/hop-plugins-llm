package org.apache.hop.langchain4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LLMMetaPlugin {
  String type();

  String typeDescription();

  String classLoaderGroup() default "";

  String documentationUrl() default "";
}