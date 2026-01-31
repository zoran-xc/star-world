package top.xcyyds.starworld.forge.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public interface CommandContributor {
    void register(LiteralArgumentBuilder<CommandSourceStack> starWorldRoot);
}
