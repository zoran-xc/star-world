package top.xcyyds.starworld.forge.nation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import top.xcyyds.starworld.common.nation.NationQuery;
import top.xcyyds.starworld.common.nation.NationQueryResult;
import top.xcyyds.starworld.common.nation.NationSavedData;
import top.xcyyds.starworld.forge.command.CommandContributor;
import top.xcyyds.starworld.forge.debug.console.StarWorldConsole;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public final class StarWorldNationCommands implements CommandContributor {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> starWorldRoot) {
        starWorldRoot.then(
                Commands.literal("nation")
                        .then(Commands.literal("regenerate")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    long t0 = System.nanoTime();
                                    NationSavedData data = NationSavedData.regenerate(level);
                                    long ms = (System.nanoTime() - t0) / 1_000_000L;
                                    StarWorldConsole.log(level, "NATION", "NationSavedData.regenerate done, nations=" + data.nations().size() + ", costMs=" + ms);
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
                                }))
        );
    }
}

