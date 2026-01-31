package top.xcyyds.starworld.forge.debug.wand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.debug.wand.DebugWandItem;
import top.xcyyds.starworld.forge.debug.wand.DebugWandOptions;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DebugWandHudForgeEvents {
    private DebugWandHudForgeEvents() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        List<String> lines = new ArrayList<>();

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DebugWandItem) {
            DebugWandOptions options = DebugWandOptions.get(main);
            if (options.enableMainHand) {
                lines.add("[调试棒-主手] nationArea=" + options.showNationArea + ", path=" + options.showPath);
            }
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof DebugWandItem) {
            DebugWandOptions options = DebugWandOptions.get(off);
            if (options.enableOffHand) {
                lines.add("[调试棒-副手] nationArea=" + options.showNationArea + ", path=" + options.showPath);
            }
        }

        if (lines.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 6;
        int y = 6;
        for (String line : lines) {
            graphics.drawString(minecraft.font, line, x, y, 0xFFFFFF, true);
            y += 10;
        }
    }
}
