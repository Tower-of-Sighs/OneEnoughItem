package com.mafuyu404.oneenoughitem.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final String CATEGORY = "key.categories.oneenoughitem";
    public static KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.oneenoughitem.open_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
}
