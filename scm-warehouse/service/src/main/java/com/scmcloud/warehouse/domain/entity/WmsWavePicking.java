package com.scmcloud.warehouse.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 波次拣货�?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_wave_picking")
public class WmsWavePicking implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("wave_no")
    private String waveNo;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("outbound_ids")
    private String outboundIds;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("picking_path")
    private String pickingPath;

    @TableField("total_distance")
    private Integer totalDistance;

    @TableField("optimization_rate")
    private BigDecimal optimizationRate;

    @TableField("status")
    private Integer status;

    @TableField("picker_id")
    private String pickerId;

    @TableField("picker_name")
    private String pickerName;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("remark")
    private String remark;


}
