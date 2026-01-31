package top.xcyyds.starworld.common.npc.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public final class NpcInventory {
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public int size() {
        return items.size();
    }

    public ItemStack get(int slot) {
        return items.get(slot);
    }

    public void set(int slot, ItemStack stack) {
        items.set(slot, stack);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("items", list);
    }

    public void load(CompoundTag tag) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        if (!tag.contains("items")) {
            return;
        }

        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, ItemStack.of(itemTag));
            }
        }
    }
}
