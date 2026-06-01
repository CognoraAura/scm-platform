package scm.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.service.impl.TenantPackageServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant-package")
public class TenantPackageController {

    @Autowired
    private TenantPackageServiceImpl tenantPackageService;

    @PostMapping
    public TenantPackage create(@RequestBody TenantPackage entity) {
        log.info("[API] 创建租户套餐: packageCode={}, packageName={}", entity.getPackageCode(), entity.getPackageName());
        return tenantPackageService.createPackage(entity);
    }

    @GetMapping("/{id}")
    public TenantPackage getById(@PathVariable String id) {
        log.info("[API] 查询租户套餐: id={}", id);
        return tenantPackageService.getById(id);
    }

    @PutMapping
    public TenantPackage update(@RequestBody TenantPackage entity) {
        log.info("[API] 更新租户套餐: id={}", entity.getId());
        return tenantPackageService.updatePackage(entity);
    }

    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        log.info("[API] 删除租户套餐: id={}", id);
        return tenantPackageService.deleteById(id);
    }

    @GetMapping("/active")
    public List<TenantPackage> listActive() {
        log.info("[API] 查询启用的租户套�?);
        return tenantPackageService.listActive();
    }

    @GetMapping("/page")
    public Page<TenantPackage> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) Integer packageLevel,
            @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询租户套餐: page={}, size={}", page, size);
        return tenantPackageService.pageQuery(page, size, packageName, packageLevel, enabled);
    }
}
