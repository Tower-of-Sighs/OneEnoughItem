package com.mafuyu404.oneenoughitem.client.gui.cache;

import com.mafuyu404.oneenoughitem.Oneenoughitem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public abstract class BaseCache {
    protected static final Path OEI_DIR = Paths.get("oei");

    protected final Path cacheFile;
    protected final int cacheVersion;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected volatile boolean initialized = false;

    protected BaseCache(String fileName, int version) {
        this.cacheFile = OEI_DIR.resolve(fileName);
        this.cacheVersion = version;
    }


    protected void initialize() {
        if (initialized) return;

        lock.writeLock().lock();
        try {
            if (initialized) return; // 双重检查

            loadFromFile();
            initialized = true;
            onInitialized();
        } finally {
            lock.writeLock().unlock();
        }
    }


    protected abstract void onInitialized();


    protected void loadFromFile() {
        if (!Files.exists(cacheFile)) {
            Oneenoughitem.LOGGER.debug("Cache file not found: {}, starting with empty cache", cacheFile);
            return;
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(cacheFile)))) {

            int version = dis.readInt();
            if (version != cacheVersion) {
                Oneenoughitem.LOGGER.warn("Cache version mismatch for {}, expected {}, got {}",
                        cacheFile, cacheVersion, version);
                onVersionMismatch(version);
                return;
            }

            loadData(dis);
            Oneenoughitem.LOGGER.debug("Loaded cache from: {}", cacheFile);

        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to load cache from: {}", cacheFile, e);
            onLoadError(e);
        }
    }

    /**
     * 版本不匹配时的处理
     */
    protected abstract void onVersionMismatch(int foundVersion);

    /**
     * 加载错误时的处理
     */
    protected abstract void onLoadError(IOException e);

    /**
     * 从输入流加载具体数据
     */
    protected abstract void loadData(DataInputStream dis) throws IOException;

    /**
     * 异步保存到文件
     */
    protected void saveToFileAsync() {
        Thread saveThread = new Thread(() -> {
            try {
                saveToFile();
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Failed to save cache asynchronously: {}", cacheFile, e);
            }
        });
        saveThread.setDaemon(true);
        saveThread.setName(getClass().getSimpleName() + "-Save");
        saveThread.start();
    }

    /**
     * 同步保存到文件
     */
    protected void saveToFile() {
        try {
            // 确保目录存在
            Files.createDirectories(cacheFile.getParent());

            // 创建临时文件，确保原子性写入
            Path tempFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");

            // 如果临时文件已存在，先删除
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }

            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile)))) {

                dos.writeInt(cacheVersion);
                saveData(dos);
                dos.flush();
            }

            // 原子性替换文件
            Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            Oneenoughitem.LOGGER.debug("Saved cache to: {}", cacheFile);

        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to save cache to: {}", cacheFile, e);
        }
    }

    /**
     * 将具体数据保存到输出流
     */
    protected abstract void saveData(DataOutputStream dos) throws IOException;

    /**
     * 清空缓存文件
     */
    protected void clearCacheFile() {
        try {
            if (Files.exists(cacheFile)) {
                Files.delete(cacheFile);
                Oneenoughitem.LOGGER.debug("Cache file cleared: {}", cacheFile);
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to clear cache file: {}", cacheFile, e);
        }
    }


    public Path getCacheFilePath() {
        return cacheFile;
    }

    /**
     * 读写锁保护的操作
     */
    protected <T> T withReadLock(Supplier<T> operation) {
        lock.readLock().lock();
        try {
            return operation.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected void withWriteLock(Runnable operation) {
        lock.writeLock().lock();
        try {
            operation.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected <T> T withWriteLock(Supplier<T> operation) {
        lock.writeLock().lock();
        try {
            return operation.get();
        } finally {
            lock.writeLock().unlock();
        }
    }
}