package top.xcyyds.starworld.forge.debug.wand.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import top.xcyyds.starworld.forge.debug.wand.client.screen.page.ConsolePage;
import top.xcyyds.starworld.forge.debug.wand.client.screen.page.DebugPage;
import top.xcyyds.starworld.forge.debug.wand.client.screen.page.MapPage;
import top.xcyyds.starworld.forge.debug.wand.client.screen.page.VisualOptionsPage;

import java.util.List;

public class DebugWandDebugScreen extends Screen {
    private static final int NAV_WIDTH = 150;
    private static final int PADDING = 12;

    private final Screen parent;
    private final InteractionHand hand;

    private NavList navList;
    private List<DebugPage> pages;
    private DebugPage active;

    private Button doneButton;

    public DebugWandDebugScreen(Screen parent, InteractionHand hand) {
        super(Component.literal("StarWorld Debug"));
        this.parent = parent;
        this.hand = hand;
    }

    public <T extends GuiEventListener & NarratableEntry> T addWidgetPublic(T widget) {
        return super.addWidget(widget);
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidgetPublic(T widget) {
        return super.addRenderableWidget(widget);
    }

    public void removeWidgetPublic(GuiEventListener widget) {
        super.removeWidget(widget);
    }

    public int contentLeft() {
        return NAV_WIDTH + PADDING * 2 + PADDING;
    }

    public int contentTop() {
        return PADDING + PADDING;
    }

    public int contentRight() {
        return width - PADDING - PADDING;
    }

    public int contentBottom() {
        return height - PADDING * 2 - 20 - PADDING;
    }

    @Override
    protected void init() {
        pages = List.of(
                new VisualOptionsPage(this, hand),
                new ConsolePage(this),
                new MapPage(this)
        );

        navList = new NavList(minecraft, NAV_WIDTH, height, PADDING, height - PADDING, 22);
        for (DebugPage page : pages) {
            navList.addPage(page);
        }
        addWidgetPublic(navList);

        setActive(pages.get(0));

        doneButton = Button.builder(Component.translatable("gui.done"), b -> onDone())
                .bounds(width - 90 - PADDING, height - 20 - PADDING, 90, 20)
                .build();
        addRenderableWidgetPublic(doneButton);
    }

    private void setActive(DebugPage page) {
        if (active != null) {
            active.onClose();
        }
        active = page;
        if (active != null) {
            active.onOpen();
        }
    }

    private void onDone() {
        if (active != null) {
            active.onClose();
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (active != null) {
            active.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xAA000000);

        super.render(graphics, mouseX, mouseY, partialTick);

        int contentLeft = NAV_WIDTH + PADDING * 2;
        int contentTop = PADDING;
        int contentRight = width - PADDING;
        int contentBottom = height - PADDING * 2 - 20;

        graphics.fill(contentLeft, contentTop, contentRight, contentBottom, 0x33000000);

        if (active != null) {
            active.render(graphics, contentLeft + PADDING, contentTop + PADDING, contentRight - PADDING, contentBottom - PADDING, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active != null && active.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (active != null && active.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        onDone();
    }

    private final class NavList extends ObjectSelectionList<NavEntry> {
        public NavList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
            setRenderHeader(false, 0);
        }

        public void addPage(DebugPage page) {
            super.addEntry(new NavEntry(page));
        }

        @Override
        public int getRowWidth() {
            return NAV_WIDTH - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return NAV_WIDTH - 6;
        }
    }

    private final class NavEntry extends ObjectSelectionList.Entry<NavEntry> {
        private final DebugPage page;

        private NavEntry(DebugPage page) {
            this.page = page;
        }

        @Override
        public Component getNarration() {
            return page.title();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int bg = (page == active) ? 0x88444444 : 0x44222222;
            graphics.fill(left, top, left + width, top + height, bg);
            graphics.drawString(font, page.title(), left + 6, top + 7, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setActive(page);
            return true;
        }
    }
}
