package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksServices;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public final class DefaultOverlayScreen extends DefaultOverlayScreenBase {
    public DefaultOverlayScreen(BeatBlocksServices services) {
        super(services);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return mouseClickedImpl(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        return mouseDraggedImpl(click.x(), click.y(), click.button(), dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return mouseReleasedImpl(click.x(), click.y(), click.button());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return keyPressedImpl(input.key(), input.scancode(), input.modifiers());
    }

    @Override
    public boolean charTyped(CharInput input) {
        return charTypedImpl(input.codepoint(), input.modifiers());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        return mouseScrolledImpl(mx, my, hAmount, vAmount);
    }
}