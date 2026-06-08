package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.mapper.InvoiceMapper;
import com.scmcloud.finance.service.IInvoiceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements IInvoiceService {

    @Override
    public List<Invoice> listByPartyId(String partyId) {
        log.debug("鎸夊線鏉ユ柟鏌ヨ鍙戠エ: partyId={}", partyId);
        LambdaQueryWrapper<Invoice> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(partyId), Invoice::getPartyId, partyId)
                .eq(Invoice::getDeleted, false)
                .orderByDesc(Invoice::getInvoiceDate);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(String id, String issuerName) {
        log.info("寮€鍏峰彂锟?id={}, issuer={}", id, issuerName);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("鍙戠エ涓嶅瓨锟?" + id);
        }
        if (invoice.getStatus() != 0) {
            throw new IllegalStateException("鍙湁鑽夌鐘舵€佺殑鍙戠エ鎵嶈兘寮€锟?褰撳墠鐘讹拷 " + invoice.getStatus());
        }

        invoice.setStatus(1);
        invoice.setIssuerName(issuerName);
        invoice.setIssueDate(LocalDate.now());
        invoice.setUpdateTime(LocalDateTime.now());
        invoice.setUpdateBy(issuerName);

        updateById(invoice);
        log.info("鍙戠エ寮€鍏锋垚锟?id={}, invoiceNo={}", id, invoice.getInvoiceNo());
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(String id) {
        log.info("浣滃簾鍙戠エ: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("鍙戠エ涓嶅瓨锟?" + id);
        }
        if (invoice.getStatus() == 3 || invoice.getStatus() == 4) {
            throw new IllegalStateException("鍙戠エ宸蹭綔搴熸垨宸茬孩锟?涓嶈兘鍐嶆浣滃簾");
        }

        invoice.setStatus(3);
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("鍙戠エ浣滃簾鎴愬姛: id={}", id);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice redFlushInvoice(String id) {
        log.info("绾㈠啿鍙戠エ: id={}", id);

        Invoice invoice = getById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("鍙戠エ涓嶅瓨锟?" + id);
        }
        if (invoice.getStatus() != 1 && invoice.getStatus() != 2) {
            throw new IllegalStateException("鍙湁宸插紑鍏锋垨宸查偖瀵勭殑鍙戠エ鎵嶈兘绾㈠啿, 褰撳墠鐘讹拷 " + invoice.getStatus());
        }

        invoice.setStatus(4);
        invoice.setUpdateTime(LocalDateTime.now());

        updateById(invoice);
        log.info("鍙戠エ绾㈠啿鎴愬姛: id={}", id);
        return invoice;
    }
}
