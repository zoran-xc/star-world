package top.xcyyds.starworld.forge.nation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.common.nation.NationQuery;
import top.xcyyds.starworld.common.nation.NationQueryResult;
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
                                        }))
                                .then(Commands.literal("where")
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerLevel level = source.getLevel();

                                            int x = (int) Math.floor(source.getPosition().x);
                                            int z = (int) Math.floor(source.getPosition().z);
                                            NationQueryResult result = NationQuery.query(level, x, z);

                                            source.sendSuccess(() -> Component.literal(
                                                    "nationId=" + result.nation().id()
                                                            + ", zhName=" + result.nation().zhName()
                                                            + ", band=" + result.borderBand()
                                                            + ", borderDist=" + String.format("%.1f", result.borderDistanceBlocks())
                                                            + ", innerStart=" + result.bufferInnerStartBlocks()
                                                            + ", bufferTotal=" + result.bufferTotalBlocks()
                                            ), false);
                                            return 1;
                                        })))
        );
    }
}
