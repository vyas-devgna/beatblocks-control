package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksServices;

public final class BeatBlocksOverlayScreen extends BeatBlocksOverlayScreenBase {
    public BeatBlocksOverlayScreen(BeatBlocksServices services) {
        super(services);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        return mouseClickedImpl(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return mouseDraggedImpl(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return mouseReleasedImpl(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        return mouseScrolledImpl(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyPressedImpl(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return charTypedImpl(chr, modifiers);
    }
}