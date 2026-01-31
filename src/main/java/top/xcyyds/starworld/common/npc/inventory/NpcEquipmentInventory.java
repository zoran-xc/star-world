package top.xcyyds.starworld.common.npc.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class NpcEquipmentInventory {
    private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    private ItemStack offhand = ItemStack.EMPTY;

    public ItemStack getArmor(EquipmentSlot slot) {
        int index = armorIndex(slot);
        return index >= 0 ? armor.get(index) : ItemStack.EMPTY;
    }

    public void setArmor(EquipmentSlot slot, ItemStack stack) {
        int index = armorIndex(slot);
        if (index >= 0) {
            armor.set(index, stack);
        }
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public void setOffhand(ItemStack stack) {
        offhand = stack;
    }

    private static int armorIndex(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> -1;
        };
    }

    public void save(CompoundTag tag) {
        ListTag armorList = new ListTag();
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                armorList.add(itemTag);
            }
        }
        tag.put("armor", armorList);
        CompoundTag offhandTag = new CompoundTag();
        offhand.save(offhandTag);
        tag.put("offhand", offhandTag);
    }

    public void load(CompoundTag tag) {
        for (int i = 0; i < armor.size(); i++) {
            armor.set(i, ItemStack.EMPTY);
        }

        if (tag.contains("armor")) {
            ListTag list = tag.getList("armor", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < armor.size()) {
                    armor.set(slot, ItemStack.of(itemTag));
                }
            }
        }
        if (tag.contains("offhand")) {
            offhand = ItemStack.of(tag.getCompound("offhand"));
        } else {
            offhand = ItemStack.EMPTY;
        }
    }
}
