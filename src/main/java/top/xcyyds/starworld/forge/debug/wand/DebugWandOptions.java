package top.xcyyds.starworld.forge.debug.wand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class DebugWandOptions {
    private static final String TAG_ROOT = "StarWorldDebugWand";

    private static final String KEY_ENABLE_MAIN_HAND = "enableMainHand";
    private static final String KEY_ENABLE_OFF_HAND = "enableOffHand";

    private static final String KEY_SHOW_NATION_AREA = "showNationArea";
    private static final String KEY_SHOW_PATH = "showPath";

    public boolean enableMainHand;
    public boolean enableOffHand;

    public boolean showNationArea;
    public boolean showPath;

    public DebugWandOptions() {
        this.enableMainHand = true;
        this.enableOffHand = false;
        this.showNationArea = false;
        this.showPath = false;
    }

    public static DebugWandOptions get(ItemStack stack) {
        DebugWandOptions options = new DebugWandOptions();
        if (stack == null) {
            return options;
        }
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        if (root == null) {
            return options;
        }

        if (root.contains(KEY_ENABLE_MAIN_HAND)) {
            options.enableMainHand = root.getBoolean(KEY_ENABLE_MAIN_HAND);
        }
        if (root.contains(KEY_ENABLE_OFF_HAND)) {
            options.enableOffHand = root.getBoolean(KEY_ENABLE_OFF_HAND);
        }
        if (root.contains(KEY_SHOW_NATION_AREA)) {
            options.showNationArea = root.getBoolean(KEY_SHOW_NATION_AREA);
        }
        if (root.contains(KEY_SHOW_PATH)) {
            options.showPath = root.getBoolean(KEY_SHOW_PATH);
        }
        return options;
    }

    public static void set(ItemStack stack, DebugWandOptions options) {
        if (stack == null || options == null) {
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putBoolean(KEY_ENABLE_MAIN_HAND, options.enableMainHand);
        root.putBoolean(KEY_ENABLE_OFF_HAND, options.enableOffHand);
        root.putBoolean(KEY_SHOW_NATION_AREA, options.showNationArea);
        root.putBoolean(KEY_SHOW_PATH, options.showPath);
        stack.addTagElement(TAG_ROOT, root);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enableMainHand);
        buf.writeBoolean(enableOffHand);
        buf.writeBoolean(showNationArea);
        buf.writeBoolean(showPath);
    }

    public static DebugWandOptions read(FriendlyByteBuf buf) {
        DebugWandOptions options = new DebugWandOptions();
        options.enableMainHand = buf.readBoolean();
        options.enableOffHand = buf.readBoolean();
        options.showNationArea = buf.readBoolean();
        options.showPath = buf.readBoolean();
        return options;
    }
}
