package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Static PNG icons from {@code assets/beatblocks/textures/gui/icons/}. */
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
    public static final int TEX = 64;

    private final Identifier texture;

    Icons(String file) {
        this.texture = Identifier.of("beatblocks", "textures/gui/icons/" + file);
    }

    public Identifier texture() {
        return texture;
    }

    public static void draw(DrawContext ctx, Icons icon, int x, int y, int size) {
        if (icon == null || size <= 0) {
            return;
        }
        GuiDrawCompat.drawTexture(ctx, icon.texture, x, y, size, size, TEX, TEX);
    }
}