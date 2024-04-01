package com.cyl.wms.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseAudit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存 数据视图对象
 *
 * @author zcc
 */
@Data
public class InventoryVO extends BaseAudit implements AreaAndItemInfo {
    /**
     * ID
     */
    private Long id;
    /**
     * 物料ID
     */
    private Long itemId;
    // 物料编号
    @Excel(name = "物料编码")
    private String itemNo;
    // 物料名称
    @Excel(name = "物料名称")
    private String itemName;
    // 物料类型
    @Excel(name = "物料类型")
    private String itemTypeName;
    /**
     * 货架id
     */
    private Long rackId;
    // 货架 名称
    private String rackName;
    private Long warehouseId;
    // 仓库 名称
    @Excel(name = "仓库")
    private String warehouseName;
    private Long areaId;
    // 库区 名称
    @Excel(name = "库区")
    private String areaName;
    /**
     * 库存
     */
    @Excel(name = "库存")
    private BigDecimal quantity;
    /**
     * 备注
     */
    @Excel(name = "备注")
    private String remark;
    private BigDecimal safetyQuantity;

    /**
     * 物料删除标识
     */
    private Integer itemDelFlag;

    /**
     * 仓库删除标识
     */
    private Integer warehouseDelFlag;

    /**
     * 生产日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime productionDate;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryDate;

    /**
     * 批次
     */
    private String batch;

    private String itemUnit;

    private String itemSpecification;

    private String itemUnitPrice;
}