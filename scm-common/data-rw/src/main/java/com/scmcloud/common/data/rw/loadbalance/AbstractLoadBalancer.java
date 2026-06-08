package com.scmcloud.common.data.rw.loadbalance;

import java.util.List;

/**
 * 璐熻浇鍧囪　鍣ㄦ娊璞″熀锟?
 * <p>
 * 鎻愪緵鍏叡鐨勭┖鍊兼鏌ュ拰鍙敤鑺傜偣杩囨护閫昏緫
 *
 * @author Deng
 * @since 2025-12-16
 */
public abstract class AbstractLoadBalancer implements SlaveLoadBalancer {

    @Override
    public final String select(List<SlaveInfo> slaves) {
        if (slaves == null || slaves.isEmpty()) {
            return null;
        }

        List<SlaveInfo> available = slaves.stream()
                .filter(SlaveInfo::available)
                .toList();

        if (available.isEmpty()) {
            return null;
        }

        return doSelect(available);
    }

    /**
     * 浠庡彲鐢ㄨ妭鐐逛腑閫夋嫨涓€锟?
     *
     * @param available 鍙敤鑺傜偣鍒楄〃锛堥潪绌猴級
     * @return 閫変腑鐨勮妭鐐瑰悕锟?
     */
    protected abstract String doSelect(List<SlaveInfo> available);

    /**
     * 鑾峰彇绗竴涓妭鐐癸紙鍏滃簳锟?
     */
    protected String getFirstName(List<SlaveInfo> available) {
        return available.getFirst().name();
    }
}
