package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsRoute;
import scm.logistics.service.ITmsRouteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-route")
public class TmsRouteController {

    @Autowired
    private ITmsRouteService routeService;

    @GetMapping("/{id}")
    public ApiResponse<TmsRoute> getById(
            @PathVariable String id) {
        TmsRoute route = routeService.getById(id);
        return ApiResponse.success(route);
    }

    @GetMapping("/no/{routeNo}")
    public ApiResponse<TmsRoute> getByRouteNo(
            @PathVariable String routeNo) {
        TmsRoute route = routeService.getByRouteNo(routeNo);
        return ApiResponse.success(route);
    }

    @GetMapping("/list")
    public ApiResponse<Page<TmsRoute>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate) {
        Page<TmsRoute> result = routeService.pageList(page, size, courierId, status, deliveryDate);
        return ApiResponse.success(result);
    }

    @GetMapping("/courier/{courierId}")
    public ApiResponse<List<TmsRoute>> listByCourierId(
            @PathVariable String courierId) {
        List<TmsRoute> list = routeService.listByCourierId(courierId);
        return ApiResponse.success(list);
    }

    @PostMapping
    public ApiResponse<TmsRoute> create(@RequestBody TmsRoute route) {
        log.info("新增配送路�? routeNo={}, courierId={}", route.getRouteNo(), route.getCourierId());
        route.setId(UUID.randomUUID().toString());
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        routeService.save(route);
        return ApiResponse.success(route);
    }

    @PutMapping("/{id}")
    public ApiResponse<TmsRoute> update(@PathVariable String id, @RequestBody TmsRoute route) {
        log.info("修改配送路�? id={}", id);
        route.setId(id);
        route.setUpdateTime(LocalDateTime.now());
        routeService.updateById(route);
        return ApiResponse.success(route);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除配送路�? id={}", id);
        routeService.removeById(id);
        return ApiResponse.success();
    }
}
