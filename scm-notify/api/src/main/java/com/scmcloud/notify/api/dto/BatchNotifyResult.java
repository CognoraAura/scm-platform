package com.scmcloud.notify.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 鎵归噺閫氱煡鍙戦€佺粨锟?
 */
@Data
@Accessors(chain = true)
public class BatchNotifyResult {

    private int totalCount;
    private int successCount;
    private int failCount;
    private String message;
}
