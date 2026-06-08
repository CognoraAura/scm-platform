package com.scmcloud.common.data.rw.routing;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 璇诲啓璺敱涓婁笅锟?
 * <p>
 * 浣跨敤 ThreadLocal 淇濆瓨褰撳墠绾跨▼鐨勮矾鐢变俊锟?
 * 鏀寔宓屽璋冪敤锛堜娇鐢ㄦ爤缁撴瀯锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingContext {
    /**
     * 璺敱绫诲瀷鏍堬紙鏀寔宓屽锟?
     */
    private static final ThreadLocal<Deque<RoutingType>> ROUTING_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 鏈€鍚庡啓鍏ユ椂闂达紙鐢ㄤ簬璇诲啓涓€鑷存€т繚璇侊級
     */
    private static final ThreadLocal<Instant> LAST_WRITE_TIME = new ThreadLocal<>();

    /**
     * 寮哄埗涓诲簱鏍囪
     */
    private static final ThreadLocal<Boolean> FORCE_MASTER = ThreadLocal.withInitial(() -> false);

    /**
     * 鎸囧畾鐨勪粠搴撳悕锟?
     */
    private static final ThreadLocal<String> SPECIFIED_SLAVE = new ThreadLocal<>();

    /**
     * 璺敱绫诲瀷
     */
    public enum RoutingType {
        /**
         * 涓诲簱
         */
        MASTER,

        /**
         * 浠庡簱
         */
        SLAVE,

        /**
         * 鑷姩锛堟牴鎹簨鍔″拰 SQL 绫诲瀷鍒ゆ柇锟?
         */
        AUTO
    }

    /**
     * 璁剧疆璺敱绫诲瀷
     */
    public static void push(RoutingType type) {
        ROUTING_STACK.get().push(type);
        log.trace("[RW-Routing] Push routing type: {}", type);
    }

    /**
     * 寮瑰嚭璺敱绫诲瀷
     */
    public static void pop() {
        Deque<RoutingType> stack = ROUTING_STACK.get();
        if (!stack.isEmpty()) {
            RoutingType popped = stack.pop();
            log.trace("[RW-Routing] Pop routing type: {}", popped);
        }
    }

    /**
     * 鑾峰彇褰撳墠璺敱绫诲瀷
     */
    public static RoutingType current() {
        Deque<RoutingType> stack = ROUTING_STACK.get();
        return stack.isEmpty() ? RoutingType.AUTO : stack.peek();
    }

    /**
     * 璁剧疆寮哄埗涓诲簱
     */
    public static void forceMaster() {
        FORCE_MASTER.set(true);
        log.trace("[RW-Routing] Force master enabled");
    }

    /**
     * 娓呴櫎寮哄埗涓诲簱
     */
    public static void clearForceMaster() {
        FORCE_MASTER.set(false);
        log.trace("[RW-Routing] Force master cleared");
    }

    /**
     * 鏄惁寮哄埗涓诲簱
     */
    public static boolean isForceMaster() {
        return Boolean.TRUE.equals(FORCE_MASTER.get());
    }

    /**
     * 璁板綍鍐欐搷浣滄椂闂达紙鐢ㄤ簬璇诲啓涓€鑷存€э級
     */
    public static void markWrite() {
        LAST_WRITE_TIME.set(Instant.now());
        log.trace("[RW-Routing] Write operation marked");
    }

    /**
     * 鑾峰彇鏈€鍚庡啓鍏ユ椂锟?
     */
    public static Instant getLastWriteTime() {
        return LAST_WRITE_TIME.get();
    }

    /**
     * 鎸囧畾浠庡簱
     */
    public static void specifySlave(String slaveName) {
        SPECIFIED_SLAVE.set(slaveName);
    }

    /**
     * 鑾峰彇鎸囧畾鐨勪粠锟?
     */
    public static String getSpecifiedSlave() {
        return SPECIFIED_SLAVE.get();
    }

    /**
     * 娓呯悊涓婁笅锟?
     */
    public static void clear() {
        ROUTING_STACK.remove();
        LAST_WRITE_TIME.remove();
        FORCE_MASTER.remove();
        SPECIFIED_SLAVE.remove();
        log.trace("[RW-Routing] Context cleared");
    }

    /**
     * 鍒ゆ柇鏄惁搴旇璧颁富锟?
     *
     * @param readMasterAfterWriteMs 鍐欏悗璇讳富搴撶殑鏃堕棿绐楀彛
     * @return true 濡傛灉搴旇璧颁富锟?
     */
    public static boolean shouldUseMaster(long readMasterAfterWriteMs) {
        // 1. 寮哄埗涓诲簱
        if (isForceMaster()) {
            return true;
        }

        // 2. 鏄惧紡鎸囧畾涓诲簱
        if (current() == RoutingType.MASTER) {
            return true;
        }

        // 3. 鍐欏悗璇讳竴鑷存€ф锟?
        Instant lastWrite = getLastWriteTime();
        if (lastWrite != null) {
            long elapsed = Instant.now().toEpochMilli() - lastWrite.toEpochMilli();
            if (elapsed < readMasterAfterWriteMs) {
                log.debug("[RW-Routing] Using master due to recent write ({}ms ago)", elapsed);
                return true;
            }
        }

        return false;
    }
}
