package top.xcyyds.starworld.forge.npc.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;

public final class StarWorldNpcEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StarWorldCommon.MOD_ID);

    public static final RegistryObject<EntityType<ForgePlayerNpcEntity>> PLAYER_NPC = ENTITY_TYPES.register("player_npc", () -> EntityType.Builder
            .of(ForgePlayerNpcEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(64)
            .build("player_npc"));

    private StarWorldNpcEntityTypes() {
    }
}
