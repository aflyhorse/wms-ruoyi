package com.ruoyi.wms.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.wms.domain.entity.ItemSku;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;



@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ItemSku.class)
public class ItemSkuVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 规格名称
     */
    @ExcelProperty(value = "规格名称")
    private String skuName;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long itemId;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String itemName;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String itemNo;

    /**
     * sku编码
     */
    @ExcelProperty(value = "sku编码")
    private String outSkuId;

    /**
     * 长(cm)
     */
    @ExcelProperty(value = "长(cm)")
    private BigDecimal length;

    /**
     * 宽(cm)
     */
    @ExcelProperty(value = "宽(cm)")
    private BigDecimal width;

    /**
     * 高(cm)
     */
    @ExcelProperty(value = "=高(cm)")
    private BigDecimal height;

    /**
     * 重量(kg)
     */
    @ExcelProperty(value = "重量(kg)")
    private BigDecimal weight;

    /**
     * 所属分类名称
     */
    @ExcelProperty(value = "所属分类名称")
    private String itemCategoryName;

    private Integer delFlag;

    private Long itemCategoryId;

}