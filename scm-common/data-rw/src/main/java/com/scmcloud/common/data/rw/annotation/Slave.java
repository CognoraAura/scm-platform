package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * 寮哄埗璧颁粠锟?
 * <p>
 * 鐢ㄤ簬鏄庣‘鍙互鎺ュ彈寤惰繜鐨勬煡璇㈠満鏅紝濡傦細
 * - 鎶ヨ〃缁熻
 * - 鎵归噺瀵煎嚭
 * - 闈炲疄鏃舵煡锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Slave {

    /**
     * 鎸囧畾浠庡簱鍚嶇О锛堝彲閫夛紝榛樿浣跨敤璐熻浇鍧囪　閫夋嫨锟?
     */
    String value() default "";

    /**
     * 鏄惁鍏佽闄嶇骇鍒颁富搴擄紙浠庡簱涓嶅彲鐢ㄦ椂锟?
     */
    boolean fallbackToMaster() default true;
}
