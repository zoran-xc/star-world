package top.xcyyds.starworld.forge.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.xcyyds.starworld.forge.debug.wand.DebugWandItem;
import top.xcyyds.starworld.forge.debug.wand.DebugWandOptions;

import java.util.function.Supplier;

public final class UpdateDebugWandOptionsC2SPacket {
    private final InteractionHand hand;
    private final DebugWandOptions options;

    public UpdateDebugWandOptionsC2SPacket(InteractionHand hand, DebugWandOptions options) {
        this.hand = hand;
        this.options = options;
    }

    public static void encode(UpdateDebugWandOptionsC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        msg.options.write(buf);
    }

    public static UpdateDebugWandOptionsC2SPacket decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        DebugWandOptions options = DebugWandOptions.read(buf);
        return new UpdateDebugWandOptionsC2SPacket(hand, options);
    }

    public static void handle(UpdateDebugWandOptionsC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof DebugWandItem)) {
                return;
            }

            DebugWandOptions.set(stack, msg.options);
        });
        ctx.get().setPacketHandled(true);
    }
}
