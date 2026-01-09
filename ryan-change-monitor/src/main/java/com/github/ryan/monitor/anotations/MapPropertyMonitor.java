package com.github.ryan.monitor.anotations;

import com.github.ryan.monitor.component.change_extractor.ChangeExtractor;
import com.github.ryan.monitor.component.change_extractor.MapChangeMessageExtractor;
import com.github.ryan.monitor.component.key_provider.KeyProvider;
import com.github.ryan.monitor.component.value_translator.DefaultNullValueTranslator;
import com.github.ryan.monitor.component.value_translator.ValueTranslator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MapPropertyMonitor {
    String staticKey() default "";

    Class<? extends KeyProvider> dynamicKeyProvider() default KeyProvider.class;

    Class<? extends ValueTranslator> valueTranslator() default DefaultNullValueTranslator.class;

    Class<? extends ChangeExtractor> changeExtractor() default MapChangeMessageExtractor.class;

    int layerPriority() default Integer.MAX_VALUE;

    int innerPriority() default Integer.MAX_VALUE;
}
