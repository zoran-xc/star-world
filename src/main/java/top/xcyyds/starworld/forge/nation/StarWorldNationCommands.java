package top.xcyyds.starworld.forge.nation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.common.nation.NationSavedData;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarWorldNationCommands {
    private StarWorldNationCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("starworld")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("nation")
                                .then(Commands.literal("regenerate")
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerLevel level = source.getLevel();
                                            NationSavedData.regenerate(level);
                                            return 1;
                                        })))
        );
    }
}
