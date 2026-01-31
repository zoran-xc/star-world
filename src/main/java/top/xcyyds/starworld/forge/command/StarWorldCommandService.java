package top.xcyyds.starworld.forge.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.ServiceLoader;

public final class StarWorldCommandService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StarWorldCommandService() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("starworld")
                .requires(src -> src.hasPermission(2));

        for (CommandContributor contributor : ServiceLoader.load(CommandContributor.class)) {
            try {
                contributor.register(root);
            } catch (Exception e) {
                LOGGER.error("Failed to register commands from contributor {}", contributor.getClass().getName(), e);
            }
        }

        event.getDispatcher().register(root);
    }
}
