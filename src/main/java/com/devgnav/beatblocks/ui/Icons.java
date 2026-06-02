package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksClient;
import com.devgnav.beatblocks.image.CoverTextureManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/** PNG icons: resized to {@link #TEX_SIZE} then uploaded as dynamic textures. */
public enum Icons {
    NOW("beatblocks_icon_now_playing"),
    PLAYLIST("beatblocks_icon_playlist"),
    LIKED("beatblocks_icon_liked_songs"),
    ALBUM("beatblocks_icon_album"),
    QUEUE("beatblocks_icon_queue"),
    DIAGNOSTICS("beatblocks_icon_diagnostics"),
    SETTINGS("beatblocks_icon_settings"),
    PLAY("beatblocks_icon_play"),
    PAUSE("beatblocks_icon_pause"),
    PREVIOUS("beatblocks_icon_previous"),
    NEXT("beatblocks_icon_next"),
    SHUFFLE("beatblocks_icon_shuffle"),
    REPEAT("beatblocks_icon_repeat"),
    REPEAT_ONE("beatblocks_icon_repeat_one"),
    VOLUME("beatblocks_icon_volume"),
    VOLUME_LOW("beatblocks_icon_volume_low"),
    VOLUME_MUTED("beatblocks_icon_volume_muted"),
    HEART("beatblocks_icon_heart");

    public static final int DISPLAY = 14;
    public static final int TEX_SIZE = DISPLAY;

    private static final String ASSET = "/assets/beatblocks/textures/gui/icons/";
    private static final Map<Icons, Identifier> TEXTURES = new EnumMap<>(Icons.class);

    private final String file;

    Icons(String file) {
        this.file = file;
    }

    public static void preloadAll() {
        for (Icons icon : values()) {
            icon.texture();
        }
    }

    public Identifier texture() {
        Identifier cached = TEXTURES.get(this);
        if (cached != null) {
            return cached;
        }
        synchronized (TEXTURES) {
            cached = TEXTURES.get(this);
            if (cached != null) {
                return cached;
            }
            byte[] png = loadDisplayPng(file);
            if (png == null) {
                return null;
            }
            Identifier uploaded = CoverTextureManager.getOrCreateTexture("icon:" + file, png);
            if (uploaded != null) {
                TEXTURES.put(this, uploaded);
            }
            return uploaded;
        }
    }

    public static void draw(DrawContext ctx, Icons icon, int x, int y, int size) {
        if (icon == null || size <= 0) {
            return;
        }
        Identifier id = icon.texture();
        if (id != null) {
            GuiDrawCompat.drawTexture(ctx, id, x, y, size, size, TEX_SIZE, TEX_SIZE);
        }
    }

    private static byte[] loadDisplayPng(String name) {
        try (InputStream in = Icons.class.getResourceAsStream(ASSET + name + ".png")) {
            if (in == null) {
                BeatBlocksClient.LOGGER.warn("BeatBlocks icon missing: {}{}.png", ASSET, name);
                return null;
            }
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                return null;
            }
            BufferedImage out = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(src, 0, 0, TEX_SIZE, TEX_SIZE, null);
            g.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            BeatBlocksClient.LOGGER.warn("BeatBlocks icon read failed: {}", name, e);
            return null;
        }
    }
}