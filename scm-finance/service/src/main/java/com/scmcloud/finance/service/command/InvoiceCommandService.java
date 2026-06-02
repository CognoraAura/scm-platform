package com.scmcloud.finance.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceCommandService {
    private final InvoiceMapper invoiceMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(String id, String issuerName) {
        log.info("开具发票: id={}, issuer={}", id, issuerName);
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的发票才能开票, 当前状态 " + invoice.getStatus());
        }
        invoice.setStatus(1);
        invoice.setIssuerName(issuerName);
        invoice.setIssueDate(LocalDate.now());
        invoice.setUpdateTime(LocalDateTime.now());
        invoice.setUpdateBy(issuerName);
        invoiceMapper.updateById(invoice);
        log.info("发票开具成功: id={}, invoiceNo={}", id, invoice.getInvoiceNo());
        return invoice;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(String id) {
        log.info("作废发票: id={}", id);
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() == 3 || invoice.getStatus() == 4) {
            throw new IllegalStateException("发票已作废或已红冲, 不能再次作废");
        }
        invoice.setStatus(3);
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        log.info("发票作废成功: id={}", id);
        return invoice;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public Invoice redFlushInvoice(String id) {
        log.info("红冲发票: id={}", id);
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
            throw new IllegalArgumentException("发票不存在: " + id);
        }
        if (invoice.getStatus() != 1 && invoice.getStatus() != 2) {
            throw new IllegalStateException("只有已开具或已邮寄的发票才能红冲, 当前状态 " + invoice.getStatus());
        }
        invoice.setStatus(4);
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        log.info("发票红冲成功: id={}", id);
        return invoice;
    }
}
