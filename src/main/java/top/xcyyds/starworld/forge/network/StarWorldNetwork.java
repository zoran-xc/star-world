package top.xcyyds.starworld.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.network.packet.ConsoleSnapshotS2CPacket;
import top.xcyyds.starworld.forge.network.packet.RequestConsoleSnapshotC2SPacket;
import top.xcyyds.starworld.forge.network.packet.UpdateDebugWandOptionsC2SPacket;

public final class StarWorldNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(StarWorldCommon.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private StarWorldNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(id++, UpdateDebugWandOptionsC2SPacket.class, UpdateDebugWandOptionsC2SPacket::encode, UpdateDebugWandOptionsC2SPacket::decode, UpdateDebugWandOptionsC2SPacket::handle);
        CHANNEL.registerMessage(id++, RequestConsoleSnapshotC2SPacket.class, RequestConsoleSnapshotC2SPacket::encode, RequestConsoleSnapshotC2SPacket::decode, RequestConsoleSnapshotC2SPacket::handle);
        CHANNEL.registerMessage(id++, ConsoleSnapshotS2CPacket.class, ConsoleSnapshotS2CPacket::encode, ConsoleSnapshotS2CPacket::decode, ConsoleSnapshotS2CPacket::handle);
    }
}
