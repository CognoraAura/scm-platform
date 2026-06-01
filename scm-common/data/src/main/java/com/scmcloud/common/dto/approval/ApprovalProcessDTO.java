package com.scmcloud.common.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 *
 *
 * @author Deng
 * createData 2025/11/3 16:35
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ApprovalProcessDTO {
    @NotNull(message = "批准状态不能为�?)
    private Boolean approved;

    private String rejectReason;

    private String comment;
}
