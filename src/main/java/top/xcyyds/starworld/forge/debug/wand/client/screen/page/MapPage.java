package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;

public final class MapPage implements DebugPage {
    public MapPage(DebugWandDebugScreen screen) {
    }

    @Override
    public Component title() {
        return Component.literal("地图（计划中）");
    }

    @Override
    public void render(GuiGraphics graphics, int left, int top, int right, int bottom, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(Minecraft.getInstance().font, Component.literal("TODO: 这里后续做国家领土地图视图"), left, top, 0xFFFFFF);
    }
}
