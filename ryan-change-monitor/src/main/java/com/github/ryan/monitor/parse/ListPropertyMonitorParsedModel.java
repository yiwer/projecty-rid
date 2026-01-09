package com.github.ryan.monitor.parse;

import com.github.ryan.monitor.component.change_extractor.ChangeExtractor;
import com.github.ryan.monitor.component.key_provider.KeyProvider;
import com.github.ryan.monitor.component.value_translator.ValueTranslator;
import com.github.ryan.version.core.BusinessData;
import lombok.Data;

@Data
public class ListPropertyMonitorParsedModel implements IMonitorParsedModel {

    private String staticKey;

    private KeyProvider<?> dynamicKeyProvider;

    private ValueTranslator valueTranslator;

    private ChangeExtractor<PropertyMonitorParsedModel> changeExtractor;

    private Class<? extends BusinessData<?>> businessDataClass;

    private boolean isIgnoreOrderChanged;

    private String fieldName;

    private Class<?> listElementClass;

    private Integer layerPriority;

    private Integer innerPriority;
}
