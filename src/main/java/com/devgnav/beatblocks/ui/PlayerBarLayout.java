package com.devgnav.beatblocks.ui;

/** Computed rectangles for the bottom player bar (no overlap between track, controls, volume). */
final class PlayerBarLayout {
    static final int ICON_BTN = 16;
    static final int PLAY_BTN = 20;
    static final int VOL_ICON = 16;
    static final int VOL_SLIDER_H = 4;
    static final int VOL_SLIDER_MAX_W = 48;
    static final int GAP = 4;

    final int trackLeft;
    final int trackRight;
    final int shuffleX, shuffleY, shuffleW, shuffleH;
    final int prevX, prevY, prevW, prevH;
    final int playX, playY, playW, playH;
    final int nextX, nextY, nextW, nextH;
    final int repeatX, repeatY, repeatW, repeatH;
    final int heartX, heartY, heartW, heartH;
    final int queueX, queueY, queueW, queueH;
    final int volumeIconX, volumeIconY, volumeIconW, volumeIconH;
    final int volumeSliderX, volumeSliderY, volumeSliderW, volumeSliderH;
    final int volumeTextX;
    final boolean showVolumeText;
    final boolean showShuffle;
    final boolean showRepeat;
    final boolean showHeart;
    final boolean showQueue;

    private PlayerBarLayout(
            int trackLeft, int trackRight,
            int shuffleX, int shuffleY, int shuffleW, int shuffleH,
            int prevX, int prevY, int prevW, int prevH,
            int playX, int playY, int playW, int playH,
            int nextX, int nextY, int nextW, int nextH,
            int repeatX, int repeatY, int repeatW, int repeatH,
            int heartX, int heartY, int heartW, int heartH,
            int queueX, int queueY, int queueW, int queueH,
            int volumeIconX, int volumeIconY, int volumeIconW, int volumeIconH,
            int volumeSliderX, int volumeSliderY, int volumeSliderW, int volumeSliderH,
            int volumeTextX, boolean showVolumeText,
            boolean showShuffle, boolean showRepeat, boolean showHeart, boolean showQueue) {
        this.trackLeft = trackLeft;
        this.trackRight = trackRight;
        this.shuffleX = shuffleX;
        this.shuffleY = shuffleY;
        this.shuffleW = shuffleW;
        this.shuffleH = shuffleH;
        this.prevX = prevX;
        this.prevY = prevY;
        this.prevW = prevW;
        this.prevH = prevH;
        this.playX = playX;
        this.playY = playY;
        this.playW = playW;
        this.playH = playH;
        this.nextX = nextX;
        this.nextY = nextY;
        this.nextW = nextW;
        this.nextH = nextH;
        this.repeatX = repeatX;
        this.repeatY = repeatY;
        this.repeatW = repeatW;
        this.repeatH = repeatH;
        this.heartX = heartX;
        this.heartY = heartY;
        this.heartW = heartW;
        this.heartH = heartH;
        this.queueX = queueX;
        this.queueY = queueY;
        this.queueW = queueW;
        this.queueH = queueH;
        this.volumeIconX = volumeIconX;
        this.volumeIconY = volumeIconY;
        this.volumeIconW = volumeIconW;
        this.volumeIconH = volumeIconH;
        this.volumeSliderX = volumeSliderX;
        this.volumeSliderY = volumeSliderY;
        this.volumeSliderW = volumeSliderW;
        this.volumeSliderH = volumeSliderH;
        this.volumeTextX = volumeTextX;
        this.showVolumeText = showVolumeText;
        this.showShuffle = showShuffle;
        this.showRepeat = showRepeat;
        this.showHeart = showHeart;
        this.showQueue = showQueue;
    }

    static PlayerBarLayout compute(int barX, int barY, int barW, int barH, int trackInfoRight) {
        int pad = 8;
        int btnY = barY + 12;
        int volY = barY + 14;

        int volTextW = 28;
        int volSliderW = Math.min(VOL_SLIDER_MAX_W, Math.max(24, barW / 12));
        int volBlockW = VOL_ICON + GAP + volSliderW + GAP + volTextW;
        int volRight = barX + barW - pad;
        int volLeft = volRight - volBlockW;

        boolean showVolText = barW >= 420;
        if (!showVolText) {
            volBlockW = VOL_ICON + GAP + volSliderW;
            volLeft = volRight - volBlockW;
        }

        int controlsRight = volLeft - 10;
        boolean showQueue = barW >= 360;
        boolean showHeart = barW >= 340;
        boolean showShuffle = barW >= 300;
        boolean showRepeat = barW >= 300;

        int w = 0;
        if (showShuffle) w += ICON_BTN + GAP;
        w += ICON_BTN + GAP + PLAY_BTN + GAP + ICON_BTN + GAP;
        if (showRepeat) w += ICON_BTN + GAP;
        if (showHeart) w += ICON_BTN + GAP;
        if (showQueue) w += ICON_BTN;

        int controlsLeft = controlsRight - w;
        if (controlsLeft < trackInfoRight + 8) {
            controlsLeft = trackInfoRight + 8;
            showQueue = false;
            showHeart = false;
            w = ICON_BTN + GAP + PLAY_BTN + GAP + ICON_BTN;
            if (showShuffle) w += ICON_BTN + GAP;
            if (showRepeat) w += ICON_BTN + GAP;
            controlsLeft = Math.max(trackInfoRight + 8, controlsRight - w);
        }
        if (controlsLeft < trackInfoRight + 8) {
            showShuffle = false;
            showRepeat = false;
            w = ICON_BTN + GAP + PLAY_BTN + GAP + ICON_BTN;
            controlsLeft = Math.max(trackInfoRight + 8, controlsRight - w);
        }

        int x = controlsLeft;
        int shuffleX = 0, shuffleY = 0, shuffleW = 0, shuffleH = 0;
        if (showShuffle) {
            shuffleX = x;
            shuffleY = btnY;
            shuffleW = ICON_BTN;
            shuffleH = ICON_BTN;
            x += ICON_BTN + GAP;
        }
        int prevX = x;
        x += ICON_BTN + GAP;
        int playX = x;
        x += PLAY_BTN + GAP;
        int nextX = x;
        x += ICON_BTN + GAP;
        int repeatX = 0, repeatY = 0, repeatW = 0, repeatH = 0;
        if (showRepeat) {
            repeatX = x;
            repeatY = btnY;
            repeatW = ICON_BTN;
            repeatH = ICON_BTN;
            x += ICON_BTN + GAP;
        }
        int heartX = 0, heartY = 0, heartW = 0, heartH = 0;
        if (showHeart) {
            heartX = x;
            heartY = btnY;
            heartW = ICON_BTN;
            heartH = ICON_BTN;
            x += ICON_BTN + GAP;
        }
        int queueX = 0, queueY = 0, queueW = 0, queueH = 0;
        if (showQueue) {
            queueX = x;
            queueY = btnY;
            queueW = ICON_BTN;
            queueH = ICON_BTN;
        }

        int volIconX = volLeft;
        int sliderX = volIconX + VOL_ICON + GAP;
        int volTextX = sliderX + volSliderW + GAP;

        return new PlayerBarLayout(
                barX + pad, Math.min(trackInfoRight, controlsLeft - 6),
                shuffleX, shuffleY, shuffleW, shuffleH,
                prevX, btnY, ICON_BTN, ICON_BTN,
                playX, barY + 10, PLAY_BTN, PLAY_BTN,
                nextX, btnY, ICON_BTN, ICON_BTN,
                repeatX, repeatY, repeatW, repeatH,
                heartX, heartY, heartW, heartH,
                queueX, queueY, queueW, queueH,
                volIconX, volY, VOL_ICON, VOL_ICON,
                sliderX, volY + 6, volSliderW, VOL_SLIDER_H,
                volTextX, showVolText,
                showShuffle, showRepeat, showHeart, showQueue);
    }
}