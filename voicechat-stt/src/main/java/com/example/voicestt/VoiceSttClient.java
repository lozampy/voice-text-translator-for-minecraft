package com.example.voicestt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;

public class VoiceSttClient implements ClientModInitializer {
    private MicRecognizer recognizer;
    private boolean wasPressed = false;

    @Override
    public void onInitializeClient() {
        KeyBindings.register();

        Path modelDir = FabricLoader.getInstance().getGameDir()
                .resolve("voicestt").resolve("model");

        try {
            if (!Files.isDirectory(modelDir) || !Files.exists(modelDir.resolve("conf"))) {
                throw new IllegalStateException(
                    "Vosk model not found. Download a small English model from " +
                    "https://alphacephei.com/vosk/models (e.g. vosk-model-small-en-us-0.15) " +
                    "and extract it into: " + modelDir.toAbsolutePath());
            }
            recognizer = new MicRecognizer(modelDir);
        } catch (Exception e) {
            System.err.println("[VoiceSTT] " + e.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || recognizer == null) return;

        boolean pressed = KeyBindings.pushToTalk.isPressed();

        if (pressed && !wasPressed) {
            client.player.sendMessage(Text.literal("🎙 Listening..."), true);
            recognizer.start(
                text -> client.execute(() -> sendAsChat(client, text)),
                err  -> client.execute(() ->
                    client.player.sendMessage(Text.literal("§c" + err), false))
            );
        } else if (!pressed && wasPressed) {
            recognizer.stop();
            client.player.sendMessage(Text.literal("⏹ Transcribing..."), true);
        }

        wasPressed = pressed;
    }

    private void sendAsChat(MinecraftClient client, String text) {
        if (client.player == null || text.isBlank()) return;
        if (text.startsWith("/")) {
            client.player.networkHandler.sendCommand(text.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(text);
        }
    }
}
