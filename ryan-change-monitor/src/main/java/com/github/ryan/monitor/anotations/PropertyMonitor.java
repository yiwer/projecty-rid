package com.github.ryan.monitor.anotations;

import com.github.ryan.monitor.component.change_extractor.ChangeExtractor;
import com.github.ryan.monitor.component.change_extractor.PropertyKeyValueMessageExtractor;
import com.github.ryan.monitor.component.key_provider.KeyProvider;
import com.github.ryan.monitor.component.value_translator.DefaultNullValueTranslator;
import com.github.ryan.monitor.component.value_translator.ValueTranslator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b> 单字段值变更监控注解，需要是单一类型（非Collection、Map类型） </b>
 *
 * @author : yvvb
 * @since : 12/23/2025
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyMonitor {

    String staticKey() default "";

    Class<? extends KeyProvider> dynamicKeyProvider() default KeyProvider.class;

    Class<? extends ValueTranslator> valueTranslator() default DefaultNullValueTranslator.class;

    Class<? extends ChangeExtractor> changeExtractor() default PropertyKeyValueMessageExtractor.class;

    int layerPriority() default Integer.MAX_VALUE;

    int innerPriority() default Integer.MAX_VALUE;
}
