package com.scmcloud.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存表实�?
 *
 * <p>存储SKU在各仓库的库存信息，包括可用库存、锁定库存、损坏库存等
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@TableName("inv_inventory")
public class Inventory {

  /**
   * 主键 ID（UUID�?
   */
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;

  /**
   * SKU ID（关联商�?SKU�?
   */
  private String skuId;

  /**
   * 仓库 ID
   */
  private String warehouseId;

  /**
   * 总库�?= 可用库存 + 锁定库存 + 损坏库存
   */
  private Integer totalStock;

  /**
   * 可用库存（可售卖�?
   */
  private Integer availableStock;

  /**
   * 锁定库存（已预占�?
   */
  private Integer lockedStock;

  /**
   * 损坏库存（不可用�?
   */
  private Integer damagedStock;

  /**
   * 安全库存（低于此值告警）
   */
  private Integer safetyStock;

  /**
   * 最大库存（预警上限�?
   */
  private Integer maxStock;

  /**
   * 库位编码
   */
  private String locationCode;

  /**
   * 平均成本
   */
  private BigDecimal averageCost;

  /**
   * 乐观锁版本号
   */
  private Integer version;

  /**
   * 最近入库时�?
   */
  private LocalDateTime lastInboundAt;

  /**
   * 最近出库时�?
   */
  private LocalDateTime lastOutboundAt;

  /**
   * 创建时间
   */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /**
   * 创建�?ID
   */
  private String createBy;

  /**
   * 更新时间
   */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  /**
   * 更新�?ID
   */
  private String updateBy;

  /**
   * 逻辑删除标记
   */
  private Boolean deleted;

  /**
   * 备注
   */
  private String remark;
}