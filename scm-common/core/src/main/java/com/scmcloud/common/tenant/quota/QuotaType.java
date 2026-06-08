package com.scmcloud.common.tenant.quota;

import lombok.Getter;

/**
 * 閰嶉绫诲瀷鏋氫妇
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Getter
public enum QuotaType {

    /**
     * 鐢ㄦ埛锟?
     */
    USERS("鐢ㄦ埛鏁?, "current_users", "max_users"),

    /**
     * 浠撳簱锟?
     */
    WAREHOUSES("浠撳簱鏁?, "current_warehouses", "max_warehouses"),

    /**
     * SKU锟?
     */
    SKUS("SKU鏁?, "current_skus", "max_skus"),

    /**
     * 姣忔棩璁㈠崟锟?
     */
    ORDERS("姣忔棩璁㈠崟鏁?, "current_orders_today", "max_orders_per_day"),

    /**
     * 瀛樺偍绌洪棿锛圙B锟?
     */
    STORAGE("瀛樺偍绌洪棿(GB)", "current_storage_gb", "max_storage_gb"),

    /**
     * 姣忔棩API璋冪敤锟?
     */
    API_CALLS("姣忔棩API璋冪敤鏁?, "current_api_calls_today", "max_api_calls_per_day");

    /**
     * 鎻忚堪
     */
    private final String description;

    /**
     * 褰撳墠浣跨敤閲忓瓧娈靛悕锛堟暟鎹簱瀛楁锟?
     */
    private final String currentField;

    /**
     * 鏈€澶ч檺棰濆瓧娈靛悕锛堟暟鎹簱瀛楁锟?
     */
    private final String maxField;

    QuotaType(String description, String currentField, String maxField) {
        this.description = description;
        this.currentField = currentField;
        this.maxField = maxField;
    }
}