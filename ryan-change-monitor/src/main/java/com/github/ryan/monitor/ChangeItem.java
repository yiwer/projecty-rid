package com.github.ryan.monitor;

import lombok.Data;

@Data
public class ChangeItem {

    private Class<?> businessDataClass;

    private String fieldName;

    private Class<?> fieldClass;

    private Object beforeValue;

    private Object afterValue;

    private String message;

    private Integer layerPriority;

    private Integer innerPriority;
}
