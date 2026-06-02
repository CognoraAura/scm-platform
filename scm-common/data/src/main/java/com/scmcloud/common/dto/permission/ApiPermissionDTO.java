package com.scmcloud.common.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * API 权限 DTO
 * 用于动态权限加载，仅包�API 权限校验所需的核心字�
 *
 * @author Deng
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiPermissionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API 路径
     */
    private String apiPath;

    /**
     * HTTP 方法 (GET, POST, PUT, DELETE �
     */
    private String httpMethod;

    /**
     * 权限编码
     */
    private String permissionCode;
}