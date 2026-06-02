package com.devgnav.beatblocks;

import com.devgnav.beatblocks.mode.ModeManager;
import com.devgnav.beatblocks.image.GuiIconRegistry;
import com.devgnav.beatblocks.ui.DefaultOverlayScreen;
import com.devgnav.beatblocks.ui.BeatBlocksHudRenderer;
import com.devgnav.beatblocks.ui.BeatBlocksOverlayScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import com.devgnav.beatblocks.compat.InputUtilCompat;
import com.devgnav.beatblocks.compat.KeyBindingCompat;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BeatBlocksClient implements ClientModInitializer {
    public static final String MOD_ID = "beatblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static BeatBlocksServices services;
    private KeyBinding openOverlayKey;
    private KeyBinding playPauseKey;
    private KeyBinding nextKey;
    private KeyBinding prevKey;

    public static BeatBlocksServices services() {
        return services;
    }

    @Override
    public void onInitializeClient() {
        services = BeatBlocksServices.bootstrap();

        // Overlay hotkey: Alt+I (configurable in Controls)
        openOverlayKey = KeyBindingHelper.registerKeyBinding(KeyBindingCompat.create(
                "key.beatblocks.open_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.beatblocks"
        ));

        // Playback hotkeys: J, K, L
        playPauseKey = KeyBindingHelper.registerKeyBinding(KeyBindingCompat.create(
                "key.beatblocks.play_pause",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.beatblocks"
        ));

        nextKey = KeyBindingHelper.registerKeyBinding(KeyBindingCompat.create(
                "key.beatblocks.next",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.beatblocks"
        ));

        prevKey = KeyBindingHelper.registerKeyBinding(KeyBindingCompat.create(
                "key.beatblocks.prev",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.beatblocks"
        ));

        // Command /sp to open the UI (mode-aware)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("sp")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.send(() -> client.setScreen(createOverlayScreen()));
                    return 1;
                }));
        });

        // HUD
        HudRenderCallback.EVENT.register(new BeatBlocksHudRenderer(services));

        // Tick handler for keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Overlay toggle (Alt+I)
            while (openOverlayKey.wasPressed()) {
                if (client.currentScreen != null) continue;
                if (services.config().requireAltModifier && !isAltDown(client)) continue;
                client.setScreen(createOverlayScreen());
            }

            // Direct playback hotkeys (only when no screen is open)
            if (client.currentScreen == null) {
                while (playPauseKey.wasPressed()) {
                    if (services.api().currentState().playing()) services.api().pause();
                    else services.api().resume();
                }
                while (nextKey.wasPressed()) {
                    services.api().next();
                }
                while (prevKey.wasPressed()) {
                    services.api().previous();
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> client.execute(GuiIconRegistry::preloadAll));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> services.close());
        LOGGER.info("BeatBlocks Control initialized — local Spicetify bridge on port {}", services.config().bridgePort);
    }

    private Screen createOverlayScreen() {
        ModeManager.Mode mode = services.modeManager().getEffectiveMode();
        if (mode == ModeManager.Mode.ENHANCED) {
            return new BeatBlocksOverlayScreen(services);
        }
        return new DefaultOverlayScreen(services);
    }

    private static boolean isAltDown(MinecraftClient client) {
        return InputUtilCompat.isKeyPressed(client, GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtilCompat.isKeyPressed(client, GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
