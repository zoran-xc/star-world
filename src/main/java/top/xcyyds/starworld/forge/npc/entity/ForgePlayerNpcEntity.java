package top.xcyyds.starworld.forge.npc.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import top.xcyyds.starworld.common.npc.entity.PlayerNpcEntity;

public class ForgePlayerNpcEntity extends PlayerNpcEntity {
    public ForgePlayerNpcEntity(EntityType<? extends ForgePlayerNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
