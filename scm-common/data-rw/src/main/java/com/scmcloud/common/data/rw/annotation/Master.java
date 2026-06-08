package com.scmcloud.common.data.rw.annotation;

import java.lang.annotation.*;

/**
 * 寮哄埗璧颁富锟?
 * <p>
 * 鐢ㄤ簬闇€瑕佽鍙栨渶鏂版暟鎹殑鍦烘櫙锛屽锟?
 * - 鍐欏悗绔嬪嵆锟?
 * - 鍏抽敭涓氬姟鏌ヨ
 * - 浜嬪姟涓殑璇绘搷锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {

    /**
     * 鍘熷洜璇存槑锛堢敤浜庢棩蹇楀拰鐩戞帶锟?
     */
    String reason() default "";
}
