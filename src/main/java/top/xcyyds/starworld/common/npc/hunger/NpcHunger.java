package top.xcyyds.starworld.common.npc.hunger;

import net.minecraft.nbt.CompoundTag;

public final class NpcHunger {
    private int foodLevel = 20;
    private float saturationLevel = 5.0F;
    private float exhaustion;

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = Math.max(0, Math.min(20, foodLevel));
    }

    public float getSaturationLevel() {
        return saturationLevel;
    }

    public void setSaturationLevel(float saturationLevel) {
        this.saturationLevel = Math.max(0.0F, Math.min(20.0F, saturationLevel));
    }

    public void addExhaustion(float exhaustion) {
        this.exhaustion = Math.max(0.0F, this.exhaustion + exhaustion);
    }

    public void tick() {
        if (exhaustion > 4.0F) {
            exhaustion -= 4.0F;
            if (saturationLevel > 0.0F) {
                saturationLevel = Math.max(0.0F, saturationLevel - 1.0F);
            } else {
                foodLevel = Math.max(0, foodLevel - 1);
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putInt("foodLevel", foodLevel);
        tag.putFloat("saturationLevel", saturationLevel);
        tag.putFloat("exhaustion", exhaustion);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("foodLevel")) {
            setFoodLevel(tag.getInt("foodLevel"));
        }
        if (tag.contains("saturationLevel")) {
            setSaturationLevel(tag.getFloat("saturationLevel"));
        }
        if (tag.contains("exhaustion")) {
            exhaustion = tag.getFloat("exhaustion");
        }
    }
}
