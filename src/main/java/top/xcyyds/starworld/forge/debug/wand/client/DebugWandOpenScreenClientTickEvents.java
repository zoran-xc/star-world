package top.xcyyds.starworld.forge.debug.wand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.debug.wand.DebugWandItem;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DebugWandOpenScreenClientTickEvents {
    private DebugWandOpenScreenClientTickEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (minecraft.screen instanceof DebugWandDebugScreen) {
            return;
        }

        if (!minecraft.options.keyUse.consumeClick()) {
            return;
        }

        InteractionHand hand = null;

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DebugWandItem) {
            hand = InteractionHand.MAIN_HAND;
        }

        ItemStack off = player.getOffhandItem();
        if (hand == null && off.getItem() instanceof DebugWandItem) {
            hand = InteractionHand.OFF_HAND;
        }

        if (hand == null) {
            return;
        }

        DebugWandClient.openDebugScreen(hand);
    }
}
