package com.scmcloud.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.notify.domain.entity.SysUserNotificationPreference;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户通知偏好�服务�
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysUserNotificationPreferenceService extends IService<SysUserNotificationPreference> {

    SysUserNotificationPreference createPreference(SysUserNotificationPreference entity);

    SysUserNotificationPreference getById(String id);

    SysUserNotificationPreference updatePreference(SysUserNotificationPreference entity);

    boolean deleteById(String id);

    List<SysUserNotificationPreference> getByUserId(String userId);

    Page<SysUserNotificationPreference> pageQuery(int page, int size, String userId,
                                                   String notificationType, String channel);
}
