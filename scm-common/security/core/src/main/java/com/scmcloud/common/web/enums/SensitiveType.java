package com.scmcloud.common.web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 15:26
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum SensitiveType {

    /**
     * 鎵嬫満鍙疯劚鏁忥細138****1234
     */
    MOBILE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),

    /**
     * 韬唤璇佸彿鑴辨晱锟?0101********1234
     */
    ID_CARD(s -> s.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2")),

    /**
     * 閭鑴辨晱锛歛bc****@example.com
     */
    EMAIL(s -> s.replaceAll("(\\w{1,3})\\w*(@.*)", "$1****$2")),

    /**
     * 濮撳悕鑴辨晱锛氬紶**
     */
    NAME(s -> {
        if (s.length() <= 1) return "*";
        if (s.length() == 2) return s.charAt(0) + "*";
        return s.charAt(0) + "**";
    }),

    /**
     * 閾惰鍗¤劚鏁忥細6222 **** **** 1234
     */
    BANK_CARD(s -> s.replaceAll("(\\d{4})\\d*(\\d{4})", "$1 **** **** $2")),

    /**
     * 鍦板潃鑴辨晱锛氫繚鐣欏墠6锟?
     */
    ADDRESS(s -> s.length() <= 6 ? s : s.substring(0, 6) + "****");

    private final Function<String, String> desensitizer;

    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return desensitizer.apply(value);
    }
}
