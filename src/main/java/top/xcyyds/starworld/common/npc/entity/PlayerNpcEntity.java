package top.xcyyds.starworld.common.npc.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import top.xcyyds.starworld.common.name.BilingualName;
import top.xcyyds.starworld.common.name.BilingualNameProvider;
import top.xcyyds.starworld.common.npc.control.NpcControlApi;
import top.xcyyds.starworld.common.npc.control.NpcControlState;
import top.xcyyds.starworld.common.npc.hunger.NpcHunger;
import top.xcyyds.starworld.common.npc.inventory.NpcEquipmentInventory;
import top.xcyyds.starworld.common.npc.inventory.NpcInventory;
import top.xcyyds.starworld.common.npc.skin.NpcSkinSourceNameProvider;
import top.xcyyds.starworld.common.npc.skin.OfficialSkinUtils;
import top.xcyyds.starworld.common.npc.skin.NpcSkinData;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerNpcEntity extends PathfinderMob implements NpcControlApi {
    private static final EntityDataAccessor<Optional<UUID>> DATA_PROFILE_ID = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_SKIN_SOURCE_NAME = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_TEXTURES_VALUE = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_TEXTURES_SIGNATURE = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SKIN_LOCKED = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SKIN_SLIM = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_NAME_ZH = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_NAME_EN = SynchedEntityData.defineId(PlayerNpcEntity.class, EntityDataSerializers.STRING);

    private final NpcControlState controlState = new NpcControlState();
    private final NpcHunger hunger = new NpcHunger();
    private final NpcInventory inventory = new NpcInventory();
    private final NpcEquipmentInventory equipmentInventory = new NpcEquipmentInventory();
    private final NpcSkinData skinData = new NpcSkinData();

    private int skinFetchCooldownTicks;

    public PlayerNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            ensureProfileId();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        if (!this.level().isClientSide && reason == MobSpawnType.SPAWN_EGG) {
            if (getNpcNameZh().isEmpty() && getNpcNameEn().isEmpty()) {
                setNpcName(BilingualNameProvider.get().generateNpcName(this.getRandom()));
            }
            if (getSkinSourceName().isEmpty()) {
                MinecraftServer server = this.level().getServer();
                setSkinSourceName(NpcSkinSourceNameProvider.get().pickSkinSourceName(this.getRandom(), server));
            }
        }

        return data;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PROFILE_ID, Optional.empty());
        this.entityData.define(DATA_SKIN_SOURCE_NAME, "");
        this.entityData.define(DATA_SKIN_TEXTURES_VALUE, "");
        this.entityData.define(DATA_SKIN_TEXTURES_SIGNATURE, "");
        this.entityData.define(DATA_SKIN_LOCKED, false);
        this.entityData.define(DATA_SKIN_SLIM, false);
        this.entityData.define(DATA_NAME_ZH, "");
        this.entityData.define(DATA_NAME_EN, "");
    }

    public UUID getNpcProfileId() {
        return this.entityData.get(DATA_PROFILE_ID).orElseGet(this::ensureProfileId);
    }

    private UUID ensureProfileId() {
        Optional<UUID> existing = this.entityData.get(DATA_PROFILE_ID);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID generated = UUID.randomUUID();
        this.entityData.set(DATA_PROFILE_ID, Optional.of(generated));
        return generated;
    }

    public String getSkinSourceName() {
        return this.entityData.get(DATA_SKIN_SOURCE_NAME);
    }

    public void setSkinSourceName(String name) {
        if (name == null) {
            name = "";
        }
        this.skinData.setSourceName(name);
        this.entityData.set(DATA_SKIN_SOURCE_NAME, name);
    }

    public String getNpcNameZh() {
        return this.entityData.get(DATA_NAME_ZH);
    }

    public String getNpcNameEn() {
        return this.entityData.get(DATA_NAME_EN);
    }

    public void setNpcName(BilingualName name) {
        if (name == null) {
            this.entityData.set(DATA_NAME_ZH, "");
            this.entityData.set(DATA_NAME_EN, "");
            return;
        }
        this.entityData.set(DATA_NAME_ZH, name.zh());
        this.entityData.set(DATA_NAME_EN, name.en());
    }

    public boolean isSkinLocked() {
        return this.entityData.get(DATA_SKIN_LOCKED);
    }

    public String getSkinTexturesValue() {
        return this.entityData.get(DATA_SKIN_TEXTURES_VALUE);
    }

    public String getSkinTexturesSignature() {
        return this.entityData.get(DATA_SKIN_TEXTURES_SIGNATURE);
    }

    public boolean isSkinSlim() {
        return this.entityData.get(DATA_SKIN_SLIM);
    }

    public void lockSkinWithTextures(String value, String signature) {
        lockSkinOnce(value, signature);
    }

    public GameProfile buildRenderGameProfile() {
        UUID id = getNpcProfileId();
        String name = getSkinSourceName();
        if (name.isEmpty()) {
            name = "starworld_npc";
        }
        GameProfile profile = new GameProfile(id, name);
        if (isSkinLocked() && !getSkinTexturesValue().isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", getSkinTexturesValue(), getSkinTexturesSignature().isEmpty() ? null : getSkinTexturesSignature()));
        }
        return profile;
    }

    public NpcHunger hunger() {
        return hunger;
    }

    public NpcInventory inventory() {
        return inventory;
    }

    public NpcEquipmentInventory equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            hunger.tick();
            tickSkinFetch();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.setSprinting(controlState.sprinting);
        this.setShiftKeyDown(controlState.sneaking);

        if (controlState.swingMainHand) {
            this.swing(InteractionHand.MAIN_HAND, true);
        }
        if (controlState.swingOffhand) {
            this.swing(InteractionHand.OFF_HAND, true);
        }

        controlState.clearTransient();
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        float forward = clampInput(controlState.forward);
        float strafe = clampInput(controlState.strafe);

        this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.zza = forward;
        this.xxa = strafe;

        if (controlState.jumping) {
            this.getJumpControl().jump();
        }

        if (controlState.sprinting && this.isInWater()) {
            this.setSwimming(true);
        }

        super.travel(travelVector);
    }

    private static float clampInput(float v) {
        if (v > 1.0F) {
            return 1.0F;
        }
        if (v < -1.0F) {
            return -1.0F;
        }
        return v;
    }

    private void tickSkinFetch() {
        if (skinFetchCooldownTicks > 0) {
            skinFetchCooldownTicks--;
            return;
        }

        if (isSkinLocked()) {
            return;
        }

        String sourceName = getSkinSourceName();
        if (sourceName.isEmpty()) {
            return;
        }

        MinecraftServer server = level().getServer();
        if (server == null) {
            return;
        }

        try {
            Property textures = OfficialSkinUtils.fetchTexturesByPlayerName(server, sourceName).orElse(null);
            if (textures == null) {
                skinFetchCooldownTicks = 200;
                return;
            }
            lockSkinOnce(textures.getValue(), textures.getSignature());
        } catch (Throwable t) {
            skinFetchCooldownTicks = 200;
        }
    }

    private void lockSkinOnce(String value, String signature) {
        if (isSkinLocked()) {
            return;
        }

        skinData.lockWithTextures(value, signature);
        this.entityData.set(DATA_SKIN_TEXTURES_VALUE, skinData.getTexturesValue());
        this.entityData.set(DATA_SKIN_TEXTURES_SIGNATURE, skinData.getTexturesSignature());
        this.entityData.set(DATA_SKIN_LOCKED, skinData.isLocked());
        this.entityData.set(DATA_SKIN_SLIM, OfficialSkinUtils.isSlimSkinTexturesValue(skinData.getTexturesValue()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putUUID("npcProfileId", getNpcProfileId());

        tag.putString("npcNameZh", getNpcNameZh());
        tag.putString("npcNameEn", getNpcNameEn());

        CompoundTag hungerTag = new CompoundTag();
        hunger.save(hungerTag);
        tag.put("npcHunger", hungerTag);

        CompoundTag invTag = new CompoundTag();
        inventory.save(invTag);
        tag.put("npcInventory", invTag);

        CompoundTag equipmentTag = new CompoundTag();
        equipmentInventory.save(equipmentTag);
        tag.put("npcEquipment", equipmentTag);

        CompoundTag skinTag = new CompoundTag();
        skinData.save(skinTag);
        tag.put("npcSkin", skinTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("npcProfileId")) {
            this.entityData.set(DATA_PROFILE_ID, Optional.of(tag.getUUID("npcProfileId")));
        } else {
            ensureProfileId();
        }

        if (tag.contains("npcNameZh")) {
            this.entityData.set(DATA_NAME_ZH, tag.getString("npcNameZh"));
        }
        if (tag.contains("npcNameEn")) {
            this.entityData.set(DATA_NAME_EN, tag.getString("npcNameEn"));
        }

        if (tag.contains("npcHunger")) {
            hunger.load(tag.getCompound("npcHunger"));
        }

        if (tag.contains("npcInventory")) {
            inventory.load(tag.getCompound("npcInventory"));
        }

        if (tag.contains("npcEquipment")) {
            equipmentInventory.load(tag.getCompound("npcEquipment"));
        }

        if (tag.contains("npcSkin")) {
            skinData.load(tag.getCompound("npcSkin"));
            this.entityData.set(DATA_SKIN_SOURCE_NAME, skinData.getSourceName());
            this.entityData.set(DATA_SKIN_TEXTURES_VALUE, skinData.getTexturesValue());
            this.entityData.set(DATA_SKIN_TEXTURES_SIGNATURE, skinData.getTexturesSignature());
            this.entityData.set(DATA_SKIN_LOCKED, skinData.isLocked());
            this.entityData.set(DATA_SKIN_SLIM, OfficialSkinUtils.isSlimSkinTexturesValue(skinData.getTexturesValue()));
        }
    }

    @Override
    public Iterable<net.minecraft.world.item.ItemStack> getArmorSlots() {
        List<net.minecraft.world.item.ItemStack> list = new ArrayList<>(4);
        list.add(equipmentInventory.getArmor(EquipmentSlot.FEET));
        list.add(equipmentInventory.getArmor(EquipmentSlot.LEGS));
        list.add(equipmentInventory.getArmor(EquipmentSlot.CHEST));
        list.add(equipmentInventory.getArmor(EquipmentSlot.HEAD));
        return list;
    }

    @Override
    public net.minecraft.world.item.ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return equipmentInventory.getArmor(slot);
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return equipmentInventory.getOffhand();
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, net.minecraft.world.item.ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            equipmentInventory.setArmor(slot, stack);
            return;
        }
        if (slot == EquipmentSlot.OFFHAND) {
            equipmentInventory.setOffhand(stack);
            return;
        }
        super.setItemSlot(slot, stack);
    }

    @Override
    public net.minecraft.world.item.ItemStack getOffhandItem() {
        return equipmentInventory.getOffhand();
    }

    @Override
    public void setMoveInput(float forward, float strafe) {
        controlState.forward = forward;
        controlState.strafe = strafe;
    }

    @Override
    public void setJumping(boolean jumping) {
        controlState.jumping = jumping;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        controlState.sprinting = sprinting;
    }

    @Override
    public void setSneaking(boolean sneaking) {
        controlState.sneaking = sneaking;
    }

    @Override
    public void swingMainHand() {
        controlState.swingMainHand = true;
    }

    @Override
    public void swingOffhand() {
        controlState.swingOffhand = true;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof Player) {
            return false;
        }
        return super.isAlliedTo(entity);
    }
}
