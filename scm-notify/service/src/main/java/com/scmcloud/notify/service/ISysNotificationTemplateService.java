package com.scmcloud.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.notify.domain.entity.SysNotificationTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 閫氱煡妯℃澘锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysNotificationTemplateService extends IService<SysNotificationTemplate> {

    SysNotificationTemplate createTemplate(SysNotificationTemplate entity);

    SysNotificationTemplate getById(String id);

    SysNotificationTemplate updateTemplate(SysNotificationTemplate entity);

    boolean deleteById(String id);

    List<SysNotificationTemplate> listByType(String channel);

    boolean enableTemplate(String id);

    boolean disableTemplate(String id);

    Page<SysNotificationTemplate> pageQuery(int page, int size, String channel,
                                             String templateCode, Integer status);
}
