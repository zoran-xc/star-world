package top.xcyyds.starworld.forge.debug.wand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;

@OnlyIn(Dist.CLIENT)
public final class DebugWandClient {
    private DebugWandClient() {
    }

    public static void openDebugScreen(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new DebugWandDebugScreen(minecraft.screen, hand));
    }
}
