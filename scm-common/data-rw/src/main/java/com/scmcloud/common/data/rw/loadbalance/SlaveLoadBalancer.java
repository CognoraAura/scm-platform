package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;

/**
 * 浠庡簱璐熻浇鍧囪　鍣ㄦ帴锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
public interface SlaveLoadBalancer {

    /**
     * 閫夋嫨涓€涓粠锟?
     *
     * @param slaves 鍙敤浠庡簱鍒楄〃
     * @return 閫変腑鐨勪粠搴撳悕锟?
     */
    String select(List<SlaveInfo> slaves);

    /**
     * 浠庡簱淇℃伅
     */
    record SlaveInfo(
            String name,
            int weight,
            int activeConnections,
            boolean available
    ) {}
}
