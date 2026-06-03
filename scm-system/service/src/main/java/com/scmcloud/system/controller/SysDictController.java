package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;
import com.scmcloud.system.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final ISysDictService dictService;

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public ApiResponse<List<SysDictType>> listTypes(
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(dictService.listDictTypes(dictCode, status));
    }

    @GetMapping("/types/code/{dictCode}")
    public ApiResponse<SysDictType> getTypeByCode(@PathVariable String dictCode) {
        return ApiResponse.success(dictService.getDictTypeByCode(dictCode));
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:add')")
    public ApiResponse<Void> createType(@RequestBody SysDictType entity) {
        entity.setId(UUID.randomUUID().toString());
        dictService.createDictType(entity);
        return ApiResponse.success(null);
    }

    @PutMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public ApiResponse<Void> updateType(@RequestBody SysDictType entity) {
        dictService.updateDictType(entity);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public ApiResponse<Void> deleteType(@PathVariable String id) {
        dictService.deleteDictType(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/items/{dictCode}")
    public ApiResponse<List<SysDictItem>> listItems(@PathVariable String dictCode) {
        return ApiResponse.success(dictService.listDictItems(dictCode));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('system:dict:add')")
    public ApiResponse<Void> createItem(@RequestBody SysDictItem entity) {
        entity.setId(UUID.randomUUID().toString());
        dictService.createDictItem(entity);
        return ApiResponse.success(null);
    }

    @PutMapping("/items")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public ApiResponse<Void> updateItem(@RequestBody SysDictItem entity) {
        dictService.updateDictItem(entity);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        dictService.deleteDictItem(id);
        return ApiResponse.success(null);
    }
}
