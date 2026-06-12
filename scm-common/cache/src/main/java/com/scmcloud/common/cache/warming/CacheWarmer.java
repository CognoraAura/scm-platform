package com.scmcloud.common.cache.warming;

import java.util.List;

public interface CacheWarmer {
    void warmCache();
    String getWarmerName();
    default int getOrder() { return 0; }
}