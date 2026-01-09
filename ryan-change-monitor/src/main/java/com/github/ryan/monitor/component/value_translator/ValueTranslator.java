package com.github.ryan.monitor.component.value_translator;

import com.github.ryan.version.core.BusinessData;

public interface ValueTranslator {

    Object translate(Object value, BusinessData<?> businessData);

}
