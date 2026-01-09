package com.github.ryan.monitor.component.change_extractor;

import com.github.ryan.monitor.ChangeItem;
import com.github.ryan.monitor.parse.ListPropertyMonitorParsedModel;
import com.github.ryan.version.core.BusinessData;

import java.util.List;

public class MapChangeMessageExtractor implements ChangeExtractor<ListPropertyMonitorParsedModel> {
    @Override
    public List<ChangeItem> extractorChangeMessage(ListPropertyMonitorParsedModel parsedMonitorModel, BusinessData<?> businessData, Object beforeValue, Object afterValue) {
        return List.of();
    }
}
