package com.github.ryan.monitor.component.change_extractor;

import com.github.ryan.monitor.ChangeItem;
import com.github.ryan.monitor.parse.IMonitorParsedModel;
import com.github.ryan.version.core.BusinessData;

import java.util.List;

public interface ChangeExtractor<T extends IMonitorParsedModel> {
    List<ChangeItem> extractorChangeMessage(T parsedMonitorModel, BusinessData<?> businessData, Object beforeValue, Object afterValue);

}
