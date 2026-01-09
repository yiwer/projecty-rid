package com.github.ryan.monitor.component.value_translator;

import com.github.ryan.facility.common.CommonUtil;
import com.github.ryan.version.core.BusinessData;

public class DefaultNullValueTranslator implements ValueTranslator {
    public static final String DEFAULT_NULL_VALUE_STR = "[--]";

    @Override
    public Object translate(Object value, BusinessData businessData) {
        if (CommonUtil.isNull(value)
                || (value instanceof CharSequence cs && CommonUtil.isBlank(cs)
                || (value instanceof Object[] arr && arr.length == 0))
        ) {
            return DEFAULT_NULL_VALUE_STR;
        } else {
            return value;
        }
    }
}