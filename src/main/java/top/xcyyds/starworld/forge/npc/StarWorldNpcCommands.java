package top.xcyyds.starworld.forge.npc;

import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import top.xcyyds.starworld.common.name.BilingualNameProvider;
import top.xcyyds.starworld.common.npc.skin.OfficialSkinUtils;
import top.xcyyds.starworld.forge.command.CommandContributor;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;
import top.xcyyds.starworld.forge.npc.registry.StarWorldNpcEntityTypes;

import java.util.Optional;
import java.util.UUID;

public final class StarWorldNpcCommands implements CommandContributor {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> starWorldRoot) {
        starWorldRoot.then(
                Commands.literal("npc")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> spawnNpc(ctx.getSource(), Optional.empty(), Optional.empty()))
                                .then(Commands.literal("name")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> spawnNpc(ctx.getSource(), Optional.of(StringArgumentType.getString(ctx, "name")), Optional.empty()))))
                                .then(Commands.literal("uuid")
                                        .then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String raw = StringArgumentType.getString(ctx, "uuid");
                                                    UUID uuid;
                                                    try {
                                                        uuid = UUID.fromString(raw);
                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendFailure(Component.literal("invalid uuid: " + raw));
                                                        return 0;
                                                    }
                                                    return spawnNpc(ctx.getSource(), Optional.empty(), Optional.of(uuid));
                                                })))
                        )
        );
    }

    private static int spawnNpc(CommandSourceStack source, Optional<String> playerName, Optional<UUID> uuid) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("player only"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        ForgePlayerNpcEntity npc = StarWorldNpcEntityTypes.PLAYER_NPC.get().create(level);
        if (npc == null) {
            source.sendFailure(Component.literal("failed to create npc"));
            return 0;
        }

        npc.setNpcName(BilingualNameProvider.get().generateNpcName(level.getRandom()));

        if (playerName.isPresent()) {
            npc.setSkinSourceName(playerName.get());
        } else if (uuid.isEmpty()) {
            npc.setSkinSourceName(OfficialSkinUtils.randomNpcSkinSourceName(level.getRandom()));
        }

        Vec3 pos = source.getPosition();
        npc.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.COMMAND, null, null);

        if (uuid.isPresent()) {
            Property textures = OfficialSkinUtils.fetchTexturesByUuid(level.getServer(), uuid.get()).orElse(null);
            if (textures == null) {
                source.sendFailure(Component.literal("textures not found for uuid"));
                return 0;
            }
            npc.lockSkinWithTextures(textures.getValue(), textures.getSignature());
        } else if (playerName.isPresent()) {
            Property textures = OfficialSkinUtils.fetchTexturesByPlayerName(level.getServer(), playerName.get()).orElse(null);
            if (textures == null) {
                source.sendFailure(Component.literal("textures not found for name"));
                return 0;
            }
            npc.lockSkinWithTextures(textures.getValue(), textures.getSignature());
        }

        level.addFreshEntity(npc);
        source.sendSuccess(() -> Component.literal("npc spawned"), false);
        return 1;
    }
}
