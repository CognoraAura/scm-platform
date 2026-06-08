package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 鍔犳潈杞璐熻浇鍧囪　锟?
 * <p>
 * 瀹炵幇骞虫粦鍔犳潈杞绠楁硶锛圢ginx 鍚屾锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
public class WeightedRoundRobinLoadBalancer extends AbstractLoadBalancer {
    /**
     * 褰撳墠鏉冮噸
     */
    private final Map<String, AtomicInteger> currentWeights = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        // 璁＄畻鎬绘潈锟?
        int totalWeight = available.stream()
                .mapToInt(SlaveInfo::weight)
                .sum();

        // 骞虫粦鍔犳潈杞
        SlaveInfo selected = null;
        int maxCurrentWeight = Integer.MIN_VALUE;

        for (SlaveInfo slave : available) {
            // 鍒濆鍖栧綋鍓嶆潈锟?
            currentWeights.computeIfAbsent(slave.name(), k -> new AtomicInteger(0));

            // 澧炲姞褰撳墠鏉冮噸
            int current = currentWeights.get(slave.name()).addAndGet(slave.weight());

            if (current > maxCurrentWeight) {
                maxCurrentWeight = current;
                selected = slave;
            }
        }

        if (selected != null) {
            // 閫変腑鐨勮妭鐐瑰噺鍘绘€绘潈锟?
            currentWeights.get(selected.name()).addAndGet(-totalWeight);
            return selected.name();
        }

        return getFirstName(available);
    }
}
