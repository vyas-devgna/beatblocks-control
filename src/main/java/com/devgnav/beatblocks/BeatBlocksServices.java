package com.devgnav.beatblocks;

import com.devgnav.beatblocks.config.BeatBlocksConfig;
import com.devgnav.beatblocks.image.ImageCacheService;
import com.devgnav.beatblocks.mode.ModeManager;
import com.devgnav.beatblocks.spotify.BeatBlocksApiClient;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class BeatBlocksServices implements AutoCloseable {
    public static final String MOD_VERSION = "1.0.0";

    private final BeatBlocksConfig config;
    private final BeatBlocksApiClient apiClient;
    private final ImageCacheService imageCache;
    private final ModeManager modeManager;
    private final ExecutorService bridgeExecutor;
    private final ExecutorService networkExecutor;

    private BeatBlocksServices(
            BeatBlocksConfig config,
            BeatBlocksApiClient apiClient,
            ImageCacheService imageCache,
            ModeManager modeManager,
            ExecutorService bridgeExecutor,
            ExecutorService networkExecutor
    ) {
        this.config = config;
        this.apiClient = apiClient;
        this.imageCache = imageCache;
        this.modeManager = modeManager;
        this.bridgeExecutor = bridgeExecutor;
        this.networkExecutor = networkExecutor;
    }

    public static BeatBlocksServices bootstrap() {
        Path baseDir = FabricLoader.getInstance().getConfigDir().resolve("beatblocks");
        BeatBlocksConfig config = BeatBlocksConfig.load(baseDir);
        ExecutorService bridgeExecutor = Executors.newCachedThreadPool(new NamedDaemonThreadFactory("beatblocks-bridge"));
        ExecutorService networkExecutor = Executors.newFixedThreadPool(4, new NamedDaemonThreadFactory("beatblocks-net"));
        BeatBlocksApiClient apiClient = new BeatBlocksApiClient(config, bridgeExecutor);
        ImageCacheService imageCache = new ImageCacheService(baseDir.resolve("covers"), config.coverPixels, config.coverCacheEntries, networkExecutor);
        ModeManager modeManager = new ModeManager(config, apiClient);
        return new BeatBlocksServices(config, apiClient, imageCache, modeManager, bridgeExecutor, networkExecutor);
    }

    public BeatBlocksConfig config() {
        return config;
    }

    public BeatBlocksApiClient api() {
        return apiClient;
    }

    public ImageCacheService imageCache() {
        return imageCache;
    }

    public ModeManager modeManager() {
        return modeManager;
    }

    @Override
    public void close() {
        apiClient.close();
        imageCache.close();
        bridgeExecutor.shutdownNow();
        networkExecutor.shutdownNow();
    }

    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger index = new AtomicInteger();

        private NamedDaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
