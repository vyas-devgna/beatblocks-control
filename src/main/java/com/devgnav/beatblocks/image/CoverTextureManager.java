package com.devgnav.beatblocks.image;

import com.devgnav.beatblocks.BeatBlocksClient;
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

    public static Identifier getOrCreateTexture(String key, byte[] pngBytes) {
        if (key == null || key.isBlank() || pngBytes == null || pngBytes.length == 0) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }

        synchronized (TEXTURE_CACHE) {
            Identifier existing = TEXTURE_CACHE.get(key);
            if (existing != null) {
                return existing;
            }
        }

        if (!client.isOnThread()) {
            client.execute(() -> getOrCreateTexture(key, pngBytes));
            synchronized (TEXTURE_CACHE) {
                return TEXTURE_CACHE.get(key);
            }
        }

        try {
            NativeImage image = NativeImageCompat.readRgbaPng(pngBytes);
            NativeImageBackedTexture texture = NativeImageCompat.createTexture(image);
            String safeKey = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
            Identifier id = Identifier.of("beatblocks", "cover_" + safeKey);

            synchronized (TEXTURE_CACHE) {
                if (TEXTURE_CACHE.containsKey(key)) {
                    return TEXTURE_CACHE.get(key);
                }
                TextureFilterCompat.setBilinear(texture);
                client.getTextureManager().registerTexture(id, texture);
                TEXTURE_CACHE.put(key, id);
                return id;
            }
        } catch (Exception e) {
            BeatBlocksClient.LOGGER.warn("BeatBlocks could not upload cover texture for {}", key, e);
            return null;
        }
    }

}