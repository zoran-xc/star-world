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

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }
}
