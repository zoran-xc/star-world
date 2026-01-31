package top.xcyyds.starworld.forge.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.xcyyds.starworld.forge.debug.console.StarWorldConsoleSavedData;
import top.xcyyds.starworld.forge.network.StarWorldNetwork;

import java.util.function.Supplier;

public final class RequestConsoleSnapshotC2SPacket {
    public RequestConsoleSnapshotC2SPacket() {
    }

    public static void encode(RequestConsoleSnapshotC2SPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestConsoleSnapshotC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestConsoleSnapshotC2SPacket();
    }

    public static void handle(RequestConsoleSnapshotC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            StarWorldConsoleSavedData data = StarWorldConsoleSavedData.getOrCreate(player.serverLevel());
            StarWorldNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConsoleSnapshotS2CPacket(data.entries()));
        });
        ctx.get().setPacketHandled(true);
    }
}
