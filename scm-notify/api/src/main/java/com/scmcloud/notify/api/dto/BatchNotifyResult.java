package com.scmcloud.notify.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量通知发送结�?
 */
@Data
@Accessors(chain = true)
public class BatchNotifyResult {

    private int totalCount;
    private int successCount;
    private int failCount;
    private String message;
}
