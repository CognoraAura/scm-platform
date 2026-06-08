package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 鍔犳潈闅忔満璐熻浇鍧囪　锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
public class WeightedRandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected String doSelect(List<SlaveInfo> available) {
        // 璁＄畻鎬绘潈锟?
        int totalWeight = available.stream()
                .mapToInt(SlaveInfo::weight)
                .sum();

        // 鐢熸垚闅忔満锟?
        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        // 鎸夋潈閲嶅尯闂撮€夋嫨
        int sum = 0;
        for (SlaveInfo slave : available) {
            sum += slave.weight();
            if (random < sum) {
                return slave.name();
            }
        }

        return getFirstName(available);
    }
}
