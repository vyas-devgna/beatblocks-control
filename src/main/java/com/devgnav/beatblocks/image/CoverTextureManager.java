package com.devgnav.beatblocks.image;

import com.devgnav.beatblocks.compat.IdentifierCompat;
import com.devgnav.beatblocks.compat.TextureFilterCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CoverTextureManager {
    private static final Map<String, Identifier> TEXTURE_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Identifier> eldest) {
            if (size() > 128) {
                Identifier id = eldest.getValue();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> client.getTextureManager().destroyTexture(id));
                }
                return true;
            }
            return false;
        }
    };

    public static Identifier getOrCreateTexture(String key, PixelCover cover) {
        if (key == null || key.isBlank() || cover == null) return null;
        synchronized (TEXTURE_CACHE) {
            Identifier existing = TEXTURE_CACHE.get(key);
            if (existing != null) return existing;

            try {
                int w = cover.width();
                int h = cover.height();
                NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        NativeImageCompat.writePixel(image, x, y, cover.colorAt(x, y));
                    }
                }

                NativeImageBackedTexture texture = NativeImageCompat.createTexture(image);
                String safeKey = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
                Identifier id = IdentifierCompat.of("beatblocks", "cover_" + safeKey);

                MinecraftClient client = MinecraftClient.getInstance();
                Runnable upload = () -> {
                    TextureFilterCompat.setBilinear(texture);
                    client.getTextureManager().registerTexture(id, texture);
                };
                if (client.isOnThread()) {
                    upload.run();
                } else {
                    client.execute(upload);
                }

                TEXTURE_CACHE.put(key, id);
                return id;
            } catch (Exception e) {
                return null;
            }
        }
    }
}