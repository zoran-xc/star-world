package top.xcyyds.starworld.forge.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import top.xcyyds.starworld.forge.debug.console.StarWorldConsoleLogEntry;
import top.xcyyds.starworld.forge.debug.console.client.StarWorldClientConsoleState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ConsoleSnapshotS2CPacket {
    private final List<StarWorldConsoleLogEntry> entries;

    public ConsoleSnapshotS2CPacket(List<StarWorldConsoleLogEntry> entries) {
        this.entries = entries;
    }

    public static void encode(ConsoleSnapshotS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (StarWorldConsoleLogEntry entry : msg.entries) {
            buf.writeLong(entry.gameTime());
            buf.writeUtf(entry.source());
            buf.writeUtf(entry.message());
        }
    }

    public static ConsoleSnapshotS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StarWorldConsoleLogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long gameTime = buf.readLong();
            String source = buf.readUtf();
            String message = buf.readUtf();
            entries.add(new StarWorldConsoleLogEntry(gameTime, source, message));
        }
        return new ConsoleSnapshotS2CPacket(entries);
    }

    public static void handle(ConsoleSnapshotS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> StarWorldClientConsoleState.setSnapshot(msg.entries));
        ctx.get().setPacketHandled(true);
    }
}
