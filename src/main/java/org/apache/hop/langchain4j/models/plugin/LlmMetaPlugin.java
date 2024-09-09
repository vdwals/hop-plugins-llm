package org.apache.hop.langchain4j.models.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation signals to the plugin system that the class is a database
 * metadata plugin.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LlmMetaPlugin {
    String type();

    String typeDescription();

    String classLoaderGroup() default "";

    String documentationUrl() default "";
}
