package top.xcyyds.starworld.common.npc.skin;

import net.minecraft.nbt.CompoundTag;

public final class NpcSkinData {
    private String sourceName = "";
    private String texturesValue = "";
    private String texturesSignature = "";
    private boolean locked;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName == null ? "" : sourceName;
    }

    public String getTexturesValue() {
        return texturesValue;
    }

    public String getTexturesSignature() {
        return texturesSignature;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lockWithTextures(String value, String signature) {
        if (locked) {
            return;
        }
        this.texturesValue = value == null ? "" : value;
        this.texturesSignature = signature == null ? "" : signature;
        this.locked = true;
    }

    public boolean hasTextures() {
        return !texturesValue.isEmpty();
    }

    public void save(CompoundTag tag) {
        tag.putString("sourceName", sourceName);
        tag.putString("texturesValue", texturesValue);
        tag.putString("texturesSignature", texturesSignature);
        tag.putBoolean("locked", locked);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("sourceName")) {
            sourceName = tag.getString("sourceName");
        }
        if (tag.contains("texturesValue")) {
            texturesValue = tag.getString("texturesValue");
        }
        if (tag.contains("texturesSignature")) {
            texturesSignature = tag.getString("texturesSignature");
        }
        if (tag.contains("locked")) {
            locked = tag.getBoolean("locked");
        }
    }
}
