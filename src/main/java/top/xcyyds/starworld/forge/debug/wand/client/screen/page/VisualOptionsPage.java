package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.xcyyds.starworld.forge.debug.wand.DebugWandOptions;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;
import top.xcyyds.starworld.forge.network.StarWorldNetwork;
import top.xcyyds.starworld.forge.network.packet.UpdateDebugWandOptionsC2SPacket;

public final class VisualOptionsPage implements DebugPage {
    private final DebugWandDebugScreen screen;
    private final InteractionHand hand;

    private Checkbox enableMainHand;
    private Checkbox enableOffHand;
    private Checkbox showNationArea;
    private Checkbox showPath;

    private Button save;

    private int left;
    private int top;

    public VisualOptionsPage(DebugWandDebugScreen screen, InteractionHand hand) {
        this.screen = screen;
        this.hand = hand;
    }

    @Override
    public Component title() {
        return Component.literal("调试棒可视化选项");
    }

    @Override
    public void onOpen() {
        this.left = screen.contentLeft();
        this.top = screen.contentTop();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        DebugWandOptions options = DebugWandOptions.get(stack);

        enableMainHand = new Checkbox(left, top, 260, 20, Component.literal("主手持有时启用"), options.enableMainHand);
        enableOffHand = new Checkbox(left, top + 24, 260, 20, Component.literal("副手持有时启用"), options.enableOffHand);
        showNationArea = new Checkbox(left, top + 48, 260, 20, Component.literal("显示国家区域（预留）"), options.showNationArea);
        showPath = new Checkbox(left, top + 72, 260, 20, Component.literal("显示寻路路径（预留）"), options.showPath);

        save = Button.builder(Component.literal("保存到调试棒"), b -> onSave())
                .bounds(left, top + 110, 120, 20)
                .build();

        screen.addRenderableWidgetPublic(enableMainHand);
        screen.addRenderableWidgetPublic(enableOffHand);
        screen.addRenderableWidgetPublic(showNationArea);
        screen.addRenderableWidgetPublic(showPath);
        screen.addRenderableWidgetPublic(save);
    }

    @Override
    public void onClose() {
        if (enableMainHand != null) {
            screen.removeWidgetPublic(enableMainHand);
            screen.removeWidgetPublic(enableOffHand);
            screen.removeWidgetPublic(showNationArea);
            screen.removeWidgetPublic(showPath);
            screen.removeWidgetPublic(save);
        }
    }

    private void onSave() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);

        DebugWandOptions options = new DebugWandOptions();
        options.enableMainHand = enableMainHand.selected();
        options.enableOffHand = enableOffHand.selected();
        options.showNationArea = showNationArea.selected();
        options.showPath = showPath.selected();

        DebugWandOptions.set(stack, options);
        StarWorldNetwork.CHANNEL.sendToServer(new UpdateDebugWandOptionsC2SPacket(hand, options));
    }

    @Override
    public void render(GuiGraphics graphics, int left, int top, int right, int bottom, int mouseX, int mouseY, float partialTick) {
        this.left = left;
        this.top = top;

        graphics.drawString(Minecraft.getInstance().font, Component.literal("配置项会写入该调试棒的 NBT（每根调试棒独立）"), left, top - 10, 0xFFFFFF);
    }
}
