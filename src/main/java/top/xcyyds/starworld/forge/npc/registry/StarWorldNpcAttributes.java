package top.xcyyds.starworld.forge.npc.registry;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import top.xcyyds.starworld.common.npc.entity.PlayerNpcEntity;

public final class StarWorldNpcAttributes {
    private StarWorldNpcAttributes() {
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(StarWorldNpcEntityTypes.PLAYER_NPC.get(), createAttributes().build());
    }

    public static Builder createAttributes() {
        return PlayerNpcEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }
}
