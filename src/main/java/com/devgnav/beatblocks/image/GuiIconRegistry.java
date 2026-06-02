package com.devgnav.beatblocks.image;

import com.devgnav.beatblocks.BeatBlocksClient;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registers GUI icon PNGs as dynamic textures (same path as album covers on 1.21.5+). */
public final class GuiIconRegistry {
    public static final int SOURCE_SIZE = 64;
    /** Uniform on-screen icon size for sidebar, transport, and volume controls. */
    public static final int DISPLAY_SIZE = 14;

    private static final String ASSET_PREFIX = "/assets/beatblocks/textures/gui/icons/";
    private static final Map<String, Identifier> REGISTERED = new ConcurrentHashMap<>();

    private static final String[] PRELOAD = {
            "beatblocks_icon_now_playing",
            "beatblocks_icon_playlist",
            "beatblocks_icon_liked_songs",
            "beatblocks_icon_album",
            "beatblocks_icon_queue",
            "beatblocks_icon_diagnostics",
            "beatblocks_icon_settings",
            "beatblocks_icon_play",
            "beatblocks_icon_pause",
            "beatblocks_icon_previous",
            "beatblocks_icon_next",
            "beatblocks_icon_shuffle",
            "beatblocks_icon_repeat",
            "beatblocks_icon_repeat_one",
            "beatblocks_icon_volume",
            "beatblocks_icon_volume_low",
            "beatblocks_icon_volume_muted",
            "beatblocks_icon_heart",
            "beatblocks_icon_back",
            "beatblocks_icon_search",
            "beatblocks_icon_plus",
            "beatblocks_icon_check",
            "beatblocks_icon_artist",
            "beatblocks_icon_heart_filled",
    };

    private GuiIconRegistry() {}

    public static void preloadAll() {
        for (String name : PRELOAD) {
            resolve(name);
        }
    }

    public static Identifier resolve(String iconName) {
        if (iconName == null || iconName.isBlank()) {
            return null;
        }
        Identifier cached = REGISTERED.get(iconName);
        if (cached != null) {
            return cached;
        }
        synchronized (GuiIconRegistry.class) {
            cached = REGISTERED.get(iconName);
            if (cached != null) {
                return cached;
            }
            byte[] png = loadPngBytes(iconName);
            if (png == null) {
                return resourceId(iconName);
            }
            Identifier uploaded = CoverTextureManager.getOrCreateTexture("gui_icon:" + iconName, png);
            if (uploaded != null) {
                REGISTERED.put(iconName, uploaded);
                return uploaded;
            }
            return resourceId(iconName);
        }
    }

    public static Set<String> registeredNames() {
        return REGISTERED.keySet();
    }

    private static byte[] loadPngBytes(String iconName) {
        String path = ASSET_PREFIX + iconName + ".png";
        try (InputStream in = GuiIconRegistry.class.getResourceAsStream(path)) {
            if (in == null) {
                BeatBlocksClient.LOGGER.warn("BeatBlocks icon asset missing: {}", path);
                return null;
            }
            return in.readAllBytes();
        } catch (Exception e) {
            BeatBlocksClient.LOGGER.warn("BeatBlocks could not read icon {}", iconName, e);
            return null;
        }
    }

    private static Identifier resourceId(String iconName) {
        return Identifier.of("beatblocks", "textures/gui/icons/" + iconName);
    }
}