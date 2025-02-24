package com.shermansplanet.otherverse;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Keybindings {

    public static final KeyMapping KEY_IMPLEMENT =
            new KeyMapping("key.implement", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_I, "key.categories.practice");

    public static final KeyMapping KEY_FAMILIAR =
            new KeyMapping("key.familiar", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R, "key.categories.practice");

    public static final KeyMapping KEY_SIGHT =
            new KeyMapping("key.sight", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_O, "key.categories.practice");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(KEY_IMPLEMENT);
        e.register(KEY_FAMILIAR);
        e.register(KEY_SIGHT);
    }
}
