package com.ruoyi.wms.service;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.constant.ServiceConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.GenerateNoUtil;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SysDictDataVo;
import com.ruoyi.system.service.SysDictTypeService;
import com.ruoyi.wms.domain.bo.ReceiptOrderBo;
import com.ruoyi.wms.domain.bo.ReceiptOrderDetailBo;
import com.ruoyi.wms.domain.entity.*;
import com.ruoyi.wms.domain.vo.ReceiptOrderVo;
import com.ruoyi.wms.mapper.ReceiptOrderDetailMapper;
import com.ruoyi.wms.mapper.ReceiptOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 入库单Service业务层处理
 *
 * @author zcc
 * @date 2024-07-19
 */
@RequiredArgsConstructor
@Service
public class ReceiptOrderService {

    private final ReceiptOrderMapper receiptOrderMapper;
    private final ReceiptOrderDetailService receiptOrderDetailService;
    private final ReceiptOrderDetailMapper receiptOrderDetailMapper;
    private final InventoryService inventoryService;
    private final InventoryDetailService inventoryDetailService;
    private final InventoryHistoryService inventoryHistoryService;
    private final SysDictTypeService dictTypeService;

    /**
     * 查询入库单
     */
    public ReceiptOrderVo queryById(Long id){
        ReceiptOrderVo receiptOrderVo = receiptOrderMapper.selectVoById(id);
        Assert.notNull(receiptOrderVo, "入库单不存在");
        receiptOrderVo.setDetails(receiptOrderDetailMapper.selectByReceiptOrderId(id));
        return receiptOrderVo;
    }

    /**
     * 查询入库单列表
     */
    public TableDataInfo<ReceiptOrderVo> queryPageList(ReceiptOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReceiptOrder> lqw = buildQueryWrapper(bo);
        Page<ReceiptOrderVo> result = receiptOrderMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询入库单列表
     */
    public List<ReceiptOrderVo> queryList(ReceiptOrderBo bo) {
        LambdaQueryWrapper<ReceiptOrder> lqw = buildQueryWrapper(bo);
        return receiptOrderMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ReceiptOrder> buildQueryWrapper(ReceiptOrderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ReceiptOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getReceiptOrderNo()), ReceiptOrder::getReceiptOrderNo, bo.getReceiptOrderNo());
        lqw.eq(bo.getReceiptOrderType() != null, ReceiptOrder::getReceiptOrderType, bo.getReceiptOrderType());
        lqw.eq(bo.getMerchantId() != null, ReceiptOrder::getMerchantId, bo.getMerchantId());
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), ReceiptOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getPayableAmount() != null, ReceiptOrder::getPayableAmount, bo.getPayableAmount());
        lqw.eq(bo.getReceiptOrderStatus() != null, ReceiptOrder::getReceiptOrderStatus, bo.getReceiptOrderStatus());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 暂存入库单
     */
    @Transactional
    public void insertByBo(ReceiptOrderBo bo) {
        // 校验入库单号唯一性
        validateReceiptOrderNo(bo.getReceiptOrderNo());
        // 创建入库单
        ReceiptOrder add = MapstructUtils.convert(bo, ReceiptOrder.class);
        receiptOrderMapper.insert(add);
        bo.setId(add.getId());
        List<ReceiptOrderDetailBo> detailBoList = bo.getDetails();
        List<ReceiptOrderDetail> addDetailList = MapstructUtils.convert(detailBoList, ReceiptOrderDetail.class);
        addDetailList.forEach(it -> {
            it.setReceiptOrderId(add.getId());
        });
        // 创建入库单明细
        receiptOrderDetailService.saveDetails(addDetailList);
    }

    /**
     * 执行入库
     */
    @Transactional
    public void doWarehousing(ReceiptOrderBo bo) {
        // 如果没暂存过
        if (Objects.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            updateByBo(bo);
        }
        List<ReceiptOrderDetailBo> mergedDetailList = mergeReceiptOrderDetail(bo.getDetails());
        List<InventoryDetail> inventoryDetailList = new LinkedList<>();
        List<Inventory> inventoryList = new LinkedList<>();
        List<InventoryHistory> inventoryHistoryList = new LinkedList<>();
        Optional<SysDictDataVo> wmsReceiptType = dictTypeService.selectDictDataByType("wms_receipt_type")
            .stream()
            .filter(it -> it.getDictValue().equals(bo.getReceiptOrderType().toString()))
            .findFirst();
        String receiptOrderType = wmsReceiptType.isEmpty() ? StringUtils.EMPTY : wmsReceiptType.get().getDictLabel();
        // 构建入库记录、入库数据、库存记录
        bo.getDetails().forEach(detail -> {
            InventoryDetail inventoryDetail = new InventoryDetail();
            inventoryDetail.setReceiptOrderId(bo.getId());
            inventoryDetail.setReceiptOrderType(receiptOrderType);
            inventoryDetail.setOrderNo(bo.getOrderNo());
            inventoryDetail.setType(ServiceConstants.InventoryDetailType.RECEIPT);
            inventoryDetail.setSkuId(detail.getSkuId());
            inventoryDetail.setWarehouseId(detail.getWarehouseId());
            inventoryDetail.setAreaId(detail.getAreaId());
            inventoryDetail.setQuantity(detail.getQuantity());
            inventoryDetail.setExpirationTime(detail.getExpirationTime());
            inventoryDetail.setAmount(detail.getAmount());
            inventoryDetailList.add(inventoryDetail);
        });
        mergedDetailList.stream().forEach(detail -> {
            Inventory inventory = new Inventory();
            inventory.setSkuId(detail.getSkuId());
            inventory.setWarehouseId(detail.getWarehouseId());
            inventory.setAreaId(detail.getAreaId());
            inventory.setQuantity(detail.getQuantity());
            inventoryList.add(inventory);

            InventoryHistory inventoryHistory = new InventoryHistory();
            inventoryHistory.setFormId(bo.getId());
            inventoryHistory.setFormType(bo.getReceiptOrderType());
            inventoryHistory.setSkuId(detail.getSkuId());
            inventoryHistory.setQuantity(detail.getQuantity());
            inventoryHistory.setWarehouseId(detail.getWarehouseId());
            inventoryHistory.setAreaId(detail.getAreaId());
            inventoryHistoryList.add(inventoryHistory);
        });
        // 创建入库记录
        inventoryDetailService.saveBatch(inventoryDetailList);
        // 操作库存
        inventoryService.saveData(inventoryList);
        // 记录库存历史
        inventoryHistoryService.saveBatch(inventoryHistoryList);
    }

    /**
     * 合并入库单详情 合并key：warehouseId_areaId_skuId
     * @param unmergedList
     * @return
     */
    private List<ReceiptOrderDetailBo> mergeReceiptOrderDetail(List<ReceiptOrderDetailBo> unmergedList) {
        Function<ReceiptOrderDetailBo, String> keyFunction = it -> it.getWarehouseId() + "_" + it.getAreaId() + "_" + it.getSkuId();
        Map<String, ReceiptOrderDetailBo> mergedMap = new HashMap<>();
        unmergedList.forEach(unmergedItem -> {
            String key = keyFunction.apply(unmergedItem);
            if (mergedMap.containsKey(key)) {
                ReceiptOrderDetailBo mergedItem = mergedMap.get(key);
                mergedItem.setQuantity(mergedItem.getQuantity().add(unmergedItem.getQuantity()));
            } else {
                ReceiptOrderDetailBo copiedUnmergedItem = new ReceiptOrderDetailBo();
                BeanUtils.copyProperties(unmergedItem, copiedUnmergedItem);
                mergedMap.put(key, copiedUnmergedItem);
            }
        });
        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 修改入库单
     */
    @Transactional
    public void updateByBo(ReceiptOrderBo bo) {
        // 更新入库单
        ReceiptOrder update = MapstructUtils.convert(bo, ReceiptOrder.class);
        receiptOrderMapper.updateById(update);
        // 删除老的
        receiptOrderDetailService.deleteByReceiptOrderId(bo.getId());
        // 创建新入库单明细
        List<ReceiptOrderDetail> addDetailList = MapstructUtils.convert(bo.getDetails(), ReceiptOrderDetail.class);
        addDetailList.forEach(it -> {
            it.setId(null);
            it.setReceiptOrderId(bo.getId());
        });
        receiptOrderDetailService.saveDetails(addDetailList);
    }

    /**
     * 入库单作废
     * @param id
     */
    public void editToInvalid(Long id) {
        LambdaUpdateWrapper<ReceiptOrder> luw = Wrappers.lambdaUpdate();
        luw.eq(ReceiptOrder::getId, id);
        luw.set(ReceiptOrder::getReceiptOrderStatus, ServiceConstants.ReceiptOrderStatus.INVALID);
        receiptOrderMapper.update(null, luw);
    }

    /**
     * 删除入库单
     */
    public void deleteById(Long id) {
        validIdBeforeDelete(id);
        receiptOrderMapper.deleteById(id);
    }

    private void validIdBeforeDelete(Long id) {
        ReceiptOrderVo receiptOrderVo = queryById(id);
        Assert.notNull(receiptOrderVo, "入库单不存在");
        if (ServiceConstants.ReceiptOrderStatus.FINISH.equals(receiptOrderVo.getReceiptOrderStatus())) {
            throw new ServiceException("入库单【" + receiptOrderVo.getReceiptOrderNo() + "】已入库，无法删除！", HttpStatus.CONFLICT.value());
        }
    }

    /**
     * 批量删除入库单
     */
    public void deleteByIds(Collection<Long> ids) {
        receiptOrderMapper.deleteBatchIds(ids);
    }

    public String generateNo() {
        LambdaQueryWrapper<ReceiptOrder> receiptOrderLqw = Wrappers.lambdaQuery();
        receiptOrderLqw.select(ReceiptOrder::getReceiptOrderNo);
        receiptOrderLqw.between(ReceiptOrder::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), LocalDateTime.of(LocalDate.now(), LocalTime.MAX));
        Set<String> noSet = receiptOrderMapper.selectList(receiptOrderLqw).stream().map(ReceiptOrder::getReceiptOrderNo).collect(Collectors.toSet());
        return GenerateNoUtil.generateNextNo(noSet);
    }

    public void validateReceiptOrderNo(String receiptOrderNo) {
        LambdaQueryWrapper<ReceiptOrder> receiptOrderLqw = Wrappers.lambdaQuery();
        receiptOrderLqw.eq(ReceiptOrder::getReceiptOrderNo, receiptOrderNo);
        ReceiptOrder receiptOrder = receiptOrderMapper.selectOne(receiptOrderLqw);
        Assert.isNull(receiptOrder, "入库单号重复，请手动修改");
    }
}