package com.scmcloud.common.data.rw.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 璇诲啓鍒嗙閰嶇疆灞烇拷
 *
 * @author Deng
 * @since 2025-12-16
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource.rw")
public class ReadWriteProperties {
    /**
     * 鏄惁鍚敤璇诲啓鍒嗙
     */
    private boolean enabled = false;

    /**
     * 鏁版嵁婧愮粍閰嶇疆
     * key: 鏁版嵁婧愮粍鍚嶇О锛堝 user, permission锟?
     * value: 涓讳粠閰嶇疆
     */
    private Map<String, DataSourceGroup> groups = new HashMap<>();

    /**
     * 鍏ㄥ眬璐熻浇鍧囪　绛栫暐
     */
    private LoadBalanceType loadBalance = LoadBalanceType.ROUND_ROBIN;

    /**
     * 澶嶅埗寤惰繜瀹瑰繊鏃堕棿锛堣秴杩囨鏃堕棿寮哄埗璧颁富搴擄級
     */
    private Duration replicationLagTolerance = Duration.ofSeconds(1);

    /**
     * 鍐欏悗璇讳富搴撴寔缁椂闂达紙瑙ｅ喅璇诲啓涓€鑷存€э級
     */
    private Duration readMasterAfterWrite = Duration.ofSeconds(2);

    /**
     * 鏄惁鍚敤鍋ュ悍妫€锟?
     */
    private boolean healthCheckEnabled = true;

    /**
     * 鍋ュ悍妫€鏌ラ棿锟?
     */
    private Duration healthCheckInterval = Duration.ofSeconds(30);

    /**
     * 杩炵画澶辫触娆℃暟鍚庢爣璁颁负涓嶅彲锟?
     */
    private int failureThreshold = 3;

    /**
     * 鏁版嵁婧愮粍閰嶇疆
     */
    @Data
    public static class DataSourceGroup {
        /**
         * 涓诲簱閰嶇疆
         */
        private DataSourceConfig master;

        /**
         * 浠庡簱閰嶇疆鍒楄〃
         */
        private List<SlaveDataSourceConfig> slaves = new ArrayList<>();

        /**
         * 鏄惁鍚敤浠庡簱锛堝彲涓存椂鍏抽棴锟?
         */
        private boolean slavesEnabled = true;

        /**
         * 璐熻浇鍧囪　绛栫暐锛堣鐩栧叏灞€锟?
         */
        private LoadBalanceType loadBalance;
    }

    /**
     * 鏁版嵁婧愰厤锟?
     */
    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";

        // HikariCP 閰嶇疆
        private int minimumIdle = 5;
        private int maximumPoolSize = 20;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
    }

    /**
     * 浠庡簱鏁版嵁婧愰厤缃紙甯︽潈閲嶏級
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SlaveDataSourceConfig extends DataSourceConfig {
        /**
         * 浠庡簱鍚嶇О锛堢敤浜庢棩蹇楀拰鐩戞帶锟?
         */
        private String name = "slave";

        /**
         * 鏉冮噸锛堢敤浜庡姞鏉冭疆璇級
         */
        private int weight = 1;

        /**
         * 鏄惁鍙敤
         */
        private boolean available = true;
    }

    /**
     * 璐熻浇鍧囪　绫诲瀷
     */
    public enum LoadBalanceType {
        /**
         * 杞
         */
        ROUND_ROBIN,

        /**
         * 鍔犳潈杞
         */
        WEIGHTED_ROUND_ROBIN,

        /**
         * 闅忔満
         */
        RANDOM,

        /**
         * 鍔犳潈闅忔満
         */
        WEIGHTED_RANDOM,

        /**
         * 鏈€灏戣繛锟?
         */
        LEAST_CONNECTIONS
    }
}
