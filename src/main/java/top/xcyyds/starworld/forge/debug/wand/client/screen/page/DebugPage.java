package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public interface DebugPage {
    Component title();

    default void onOpen() {
    }

    default void onClose() {
    }

    default void tick() {
    }

    void render(GuiGraphics graphics, int left, int top, int right, int bottom, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }
}
