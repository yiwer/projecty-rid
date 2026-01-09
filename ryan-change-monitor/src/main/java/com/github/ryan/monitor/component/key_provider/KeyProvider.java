package com.github.ryan.monitor.component.key_provider;

import com.github.ryan.version.core.BusinessData;

public interface KeyProvider<D extends BusinessData<D>> {

    String provideKey(String originKey, D businessData);
}
