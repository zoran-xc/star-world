package top.xcyyds.starworld.common.npc.skin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public interface NpcSkinSourceNameProvider {
    AtomicReference<NpcSkinSourceNameProvider> INSTANCE = new AtomicReference<>(new DefaultNpcSkinSourceNameProvider());

    static NpcSkinSourceNameProvider get() {
        return INSTANCE.get();
    }

    static void set(NpcSkinSourceNameProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        INSTANCE.set(provider);
    }

    String pickSkinSourceName(RandomSource random, @Nullable MinecraftServer server);

    final class DefaultNpcSkinSourceNameProvider implements NpcSkinSourceNameProvider {
        @Override
        public String pickSkinSourceName(RandomSource random, @Nullable MinecraftServer server) {
            return "";
        }
    }
}
