package com.example.voicestt;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static KeyBinding pushToTalk;

    public static void register() {
        pushToTalk = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voicestt.ptt",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.voicestt"
        ));
    }
}
