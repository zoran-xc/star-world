package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import top.xcyyds.starworld.forge.debug.console.StarWorldConsoleLogEntry;
import top.xcyyds.starworld.forge.debug.console.client.StarWorldClientConsoleState;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;
import top.xcyyds.starworld.forge.network.StarWorldNetwork;
import top.xcyyds.starworld.forge.network.packet.RequestConsoleSnapshotC2SPacket;

import java.util.List;

public final class ConsolePage implements DebugPage {
    private final DebugWandDebugScreen screen;

    private int left;
    private int top;
    private int right;
    private int bottom;

    private int lastLeft;
    private int lastTop;
    private int lastRight;
    private int lastBottom;

    private ConsoleList list;

    public ConsolePage(DebugWandDebugScreen screen) {
        this.screen = screen;
    }

    @Override
    public Component title() {
        return Component.literal("控制台");
    }

    @Override
    public void onOpen() {
        this.left = screen.contentLeft();
        this.top = screen.contentTop();
        this.right = screen.contentRight();
        this.bottom = screen.contentBottom();

        this.lastLeft = left;
        this.lastTop = top;
        this.lastRight = right;
        this.lastBottom = bottom;

        list = new ConsoleList(Minecraft.getInstance(), right - left, bottom - top, top, bottom, 12);
        list.setLeftPos(left);
        screen.addRenderableWidgetPublic(list);

        refreshFromState();
        StarWorldNetwork.CHANNEL.sendToServer(new RequestConsoleSnapshotC2SPacket());
    }

    @Override
    public void onClose() {
        if (list != null) {
            screen.removeWidgetPublic(list);
            list = null;
        }
    }

    private void refreshFromState() {
        if (list == null) {
            return;
        }
        list.clearEntriesPublic();
        List<StarWorldConsoleLogEntry> entries = StarWorldClientConsoleState.getSnapshot();
        for (int i = 0; i < entries.size(); i++) {
            list.addEntryPublic(new ConsoleEntry(entries.get(i)));
        }
    }

    @Override
    public void tick() {
        refreshFromState();
    }

    @Override
    @SuppressWarnings("null")
    public void render(GuiGraphics graphics, int left, int top, int right, int bottom, int mouseX, int mouseY, float partialTick) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;

        Component title = Component.literal("服务端存档级持久化日志（打开时同步快照）");
        graphics.drawString(Minecraft.getInstance().font, title, left, top - 10, 0xFFFFFF);

        if (list != null) {
            if (lastLeft != left || lastTop != top || lastRight != right || lastBottom != bottom) {
                screen.removeWidgetPublic(list);
                list = new ConsoleList(Minecraft.getInstance(), right - left, bottom - top, top, bottom, 12);
                list.setLeftPos(left);
                screen.addRenderableWidgetPublic(list);
                refreshFromState();

                lastLeft = left;
                lastTop = top;
                lastRight = right;
                lastBottom = bottom;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (list != null) {
            return list.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }

    private static final class ConsoleList extends ObjectSelectionList<ConsoleEntry> {
        public ConsoleList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
            setRenderHeader(false, 0);
        }

        public void clearEntriesPublic() {
            super.clearEntries();
        }

        public void addEntryPublic(ConsoleEntry entry) {
            super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }
    }

    private static final class ConsoleEntry extends ObjectSelectionList.Entry<ConsoleEntry> {
        private final StarWorldConsoleLogEntry entry;

        private ConsoleEntry(StarWorldConsoleLogEntry entry) {
            this.entry = entry;
        }

        @Override
        public Component getNarration() {
            return Component.literal(entry.source() + ": " + entry.message());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            String text = "[" + entry.gameTime() + "] [" + entry.source() + "] " + entry.message();
            graphics.drawString(Minecraft.getInstance().font, text, left + 2, top + 1, 0xE0E0E0);
        }
    }
}
