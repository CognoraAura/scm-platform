package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantFeature;
import scm.tenant.service.impl.TenantFeatureServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-feature")
public class TenantFeatureController {

    @Autowired
    private TenantFeatureServiceImpl tenantFeatureService;

    @PostMapping
    public TenantFeature create(@RequestBody TenantFeature entity) {
        log.info("[API] 创建租户功能: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        return tenantFeatureService.createFeature(entity);
    }

    @GetMapping("/{id}")
    public TenantFeature getById(@PathVariable String id) {
        log.info("[API] 查询租户功能: id={}", id);
        return tenantFeatureService.getById(id);
    }

    @PutMapping
    public TenantFeature update(@RequestBody TenantFeature entity) {
        log.info("[API] 更新租户功能: id={}", entity.getId());
        return tenantFeatureService.updateFeature(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户功能: id={}", id);
        return tenantFeatureService.deleteById(id);
    }

    @GetMapping("/check")
    public boolean isFeatureEnabled(
            @RequestParam String tenantId,
            @RequestParam String featureCode) {
        log.info("[API] 检查功能是否启�? tenantId={}, featureCode={}", tenantId, featureCode);
        return tenantFeatureService.isFeatureEnabled(tenantId, featureCode);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<TenantFeature> listByTenantId(@PathVariable String tenantId) {
        log.info("[API] 查询租户功能列表: tenantId={}", tenantId);
        return tenantFeatureService.listByTenantId(tenantId);
    }

    @GetMapping("/page")
    public Page<TenantFeature> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String featureCode,
            @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询租户功能: page={}, size={}", page, size);
        return tenantFeatureService.pageQuery(page, size, tenantId, featureCode, enabled);
    }
}
