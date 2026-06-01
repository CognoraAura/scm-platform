package com.scmcloud.common.rest.reload;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RestClient 证书热更新加载器
 * <p>支持零停�?mTLS 证书更新</p>
 *
 * <p>功能�?
 * <ul>
 *   <li>定时检测证书文件变化（每分钟检查一次）</li>
 *   <li>自动重新加载证书（无需重启应用�?/li>
 *   <li>线程安全�?SSLContext 更新</li>
 *   <li>监听器模式：支持其他组件订阅证书更新事件</li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateReloaderRestClient {

    private final Resource keystoreResource;
    private final String keystorePassword;
    private final Resource truststoreResource;
    private final String truststorePassword;

    // 当前 SSLContext（线程安全的原子引用�?
    private final AtomicReference<SSLContext> sslContextRef = new AtomicReference<>();

    // 证书文件最后修改时间缓�?
    private volatile FileTime lastKeystoreModified;
    private volatile FileTime lastTruststoreModified;

    // 监听器列表（线程安全�?
    private final CopyOnWriteArrayList<CertificateReloadListener> listeners =
        new CopyOnWriteArrayList<>();

    /**
     * 初始化加载证�?
     */
    @PostConstruct
    public void initialize() throws Exception {
        log.info("Initializing RestClient certificates...");
        loadCertificates(true);
        updateLastModifiedTimes();
        log.info("RestClient certificates initialized successfully");
    }

    /**
     * 定时检查证书文件变化（每分钟检查一次）
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void checkAndReload() {
        try {
            boolean keystoreChanged = hasFileChanged(keystoreResource, lastKeystoreModified);
            boolean truststoreChanged = hasFileChanged(truststoreResource, lastTruststoreModified);

            if (keystoreChanged || truststoreChanged) {
                log.info("Certificate file changes detected, starting hot reload...");
                loadCertificates(false);
                updateLastModifiedTimes();
                notifyListeners();
                log.info("Certificate hot reload successful (no restart required)");
            }
        } catch (Exception e) {
            log.error("Certificate hot reload failed, continuing with old certificates: {}",
                      e.getMessage(), e);
        }
    }

    /**
     * 加载证书并构�?SSLContext
     *
     * @param isInitial 是否为初始化加载
     */
    private void loadCertificates(boolean isInitial) throws Exception {
        // 加载 KeyStore �?TrustStore
        KeyStore keyStore = loadKeyStore(keystoreResource, keystorePassword);
        KeyStore trustStore = loadKeyStore(truststoreResource, truststorePassword);

        // 初始�?KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 初始�?TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init(trustStore);

        // 创建 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            kmf.getKeyManagers(),
            tmf.getTrustManagers(),
            new SecureRandom()
        );

        // 原子性更�?SSLContext
        sslContextRef.set(sslContext);

        if (!isInitial) {
            log.info("SSLContext updated successfully");
        }
    }

    /**
     * 获取当前 SSLContext
     *
     * @return 当前�?SSLContext
     */
    public SSLContext getSslContext() {
        return sslContextRef.get();
    }

    /**
     * 注册证书重载监听�?
     *
     * @param listener 监听�?
     */
    public void addListener(CertificateReloadListener listener) {
        listeners.add(listener);
        log.debug("Certificate reload listener registered: {}", listener.getClass().getSimpleName());
    }

    /**
     * 移除证书重载监听�?
     *
     * @param listener 监听�?
     */
    public void removeListener(CertificateReloadListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器证书已重�?
     */
    private void notifyListeners() {
        SSLContext sslContext = getSslContext();
        for (CertificateReloadListener listener : listeners) {
            try {
                listener.onCertificateReloaded(sslContext);
            } catch (Exception e) {
                log.error("Failed to notify listener {}: {}",
                          listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 检查文件是否已变化
     *
     * @param resource 资源
     * @param lastModified 上次修改时间
     * @return true 如果文件已变�?
     */
    private boolean hasFileChanged(Resource resource, FileTime lastModified) {
        try {
            Path path = getPathFromResource(resource);
            if (path == null) {
                return false;
            }

            FileTime currentModified = Files.getLastModifiedTime(path);
            return lastModified == null || currentModified.compareTo(lastModified) > 0;
        } catch (IOException e) {
            log.warn("Failed to check file modification time: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 更新最后修改时间缓�?
     */
    private void updateLastModifiedTimes() {
        try {
            Path keystorePath = getPathFromResource(keystoreResource);
            if (keystorePath != null) {
                lastKeystoreModified = Files.getLastModifiedTime(keystorePath);
            }

            Path truststorePath = getPathFromResource(truststoreResource);
            if (truststorePath != null) {
                lastTruststoreModified = Files.getLastModifiedTime(truststorePath);
            }
        } catch (IOException e) {
            log.warn("Failed to update last modified times: {}", e.getMessage());
        }
    }

    /**
     * �?Resource 获取 Path
     */
    private Path getPathFromResource(Resource resource) {
        try {
            if (resource.isFile()) {
                return resource.getFile().toPath();
            } else if (resource.getURL().getProtocol().equals("file")) {
                return Paths.get(resource.getURL().toURI());
            }
        } catch (Exception e) {
            log.debug("Resource is not a file: {}", resource.getDescription());
        }
        return null;
    }

    /**
     * 加载 KeyStore
     *
     * @param resource 证书文件资源
     * @param password 密码
     * @return KeyStore
     */
    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    /**
     * 证书重载监听器接�?
     */
    @FunctionalInterface
    public interface CertificateReloadListener {
        /**
         * 证书重载回调
         *
         * @param sslContext 新的 SSLContext
         */
        void onCertificateReloaded(SSLContext sslContext);
    }
}
