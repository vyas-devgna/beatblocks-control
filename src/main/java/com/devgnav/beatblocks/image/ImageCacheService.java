package com.devgnav.beatblocks.image;

import com.devgnav.beatblocks.BeatBlocksClient;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class ImageCacheService implements AutoCloseable {
    private final Path cacheDir;
    private final int pixelSize;
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private final Map<String, CompletableFuture<PixelCover>> memory;

    public ImageCacheService(Path cacheDir, int pixelSize, int maxEntries, ExecutorService executor) {
        this.cacheDir = cacheDir;
        this.pixelSize = pixelSize;
        this.executor = executor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.memory = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CompletableFuture<PixelCover>> eldest) {
                return size() > maxEntries;
            }
        });
    }

    public CompletableFuture<PixelCover> getCover(String url) {
        if (url == null || url.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (memory) {
            CompletableFuture<PixelCover> existing = memory.get(url);
            if (existing != null) return existing;
            CompletableFuture<PixelCover> created = CompletableFuture.supplyAsync(() -> loadOrDownload(url), executor);
            memory.put(url, created);
            return created;
        }
    }

    private PixelCover loadOrDownload(String url) {
        try {
            Files.createDirectories(cacheDir);
            Path cached = cacheDir.resolve(sha256(url) + "-smooth-" + pixelSize + ".png");
            if (Files.exists(cached)) {
                return readPixelCover(ImageIO.read(cached.toFile()));
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Cover download HTTP " + response.statusCode());
            }
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (original == null) throw new IOException("Unsupported image format");
            BufferedImage resized = resizeSmooth(original);
            ImageIO.write(resized, "png", cached.toFile());
            return readPixelCover(resized);
        } catch (Exception e) {
            BeatBlocksClient.LOGGER.debug("Could not load BeatBlocks cover {}", url, e);
            return null;
        }
    }

    private BufferedImage resizeSmooth(BufferedImage original) {
        BufferedImage out = new BufferedImage(pixelSize, pixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(original, 0, 0, pixelSize, pixelSize, null);
        graphics.dispose();
        return out;
    }

    private static PixelCover readPixelCover(BufferedImage image) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return new PixelCover(width, height, pixels);
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) builder.append(String.format("%02x", b));
        return builder.toString();
    }

    @Override
    public void close() {
        memory.clear();
    }
}
